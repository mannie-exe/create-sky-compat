/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
(() => {
"use strict";
// Render loop adapted from BlueMap 5.7 MapViewer.js; bounded adapter below.
function renderDetail(delta, cutoff) {
		window.BlueMap.dispatchEvent(this.events, "bluemapRenderFrame", {
			delta: delta,
		});

		// render
		this.renderer.clear();

		// prepare skybox camera
		this.skyboxCamera.rotation.copy(this.camera.rotation);

		// render skybox
		this.renderer.render(this.skyboxScene, this.skyboxCamera);
		this.renderer.clearDepth();

		if (this.map && this.map.isLoaded) {

			this.map.animations.forEach(animation => animation.step(delta))

			// shift whole scene including camera towards 0,0 to tackle shader-precision issues
			const s = 10000;
			const sX = Math.round(this.camera.position.x / s) * s;
			const sZ = Math.round(this.camera.position.z / s) * s;
			this.camera.position.x -= sX;
			this.camera.position.z -= sZ;

			// update uniforms
			this.data.uniforms.distance.value = this.controlsManager.distance;
			this.data.uniforms.hiresTileMap.value.pos.copy(this.map.hiresTileManager.centerTile);
			this.data.uniforms.hiresTileMap.value.translate.set(
				this.map.data.hires.translate.x - sX,
				this.map.data.hires.translate.z - sZ
			);

			// prepare camera for lowres
			const originalFar = this.camera.far;
			const cameraFar = Math.max(originalFar, this.controlsManager.distance + 5000);
			this.camera.far = cameraFar;
			if (this.controlsManager.distance < cutoff) {
				this.camera.far = 1000000; // disable far clipping for lowres
			}
			this.camera.updateProjectionMatrix();

			// render lowres
			const highestLod = this.map.lowresTileManager.length - 1;
			for (let i = this.map.lowresTileManager.length - 1; i >= 0; i--) {
				if (i === highestLod || this.controlsManager.distance < 1000 * Math.pow(this.map.data.lowres.lodFactor, i + 1)) {
					let scenePos = this.map.lowresTileManager[i].scene.position;
					scenePos.x = -sX;
					scenePos.z = -sZ;

					if (i === 0) {
						this.camera.far = cameraFar; // reset far clipping for the highest lowres lod to make depth-tests possible
						this.camera.updateProjectionMatrix();
					}

					this.renderer.render(this.map.lowresTileManager[i].sceneParent, this.camera);

					if (i !== 0) this.renderer.clearDepth(); // clear depth-buffer for all lowres except the highest
				}
			}

			this.camera.far = cameraFar; // reset far clipping

			// render hires
			if (this.controlsManager.distance < cutoff) {
				this.camera.updateProjectionMatrix();
				let scenePos = this.map.hiresTileManager.scene.position;
				scenePos.x = -sX;
				scenePos.z = -sZ;
				this.renderer.render(this.map.hiresTileManager.sceneParent, this.camera);
			}

			this.camera.far = originalFar;
			this.camera.updateProjectionMatrix();

			// shift back
			this.camera.position.x += sX;
			this.camera.position.z += sZ;

		}

		// render markers
		this.renderer.render(this.markers, this.camera);
		this.css2dRenderer.render(this.markers, this.camera);
	}


    const previous = window.createSkyBlueMap;
    previous?.dispose();
    const app = window.bluemap;
    const api = window.BlueMap;
    const viewer = app?.mapViewer;
    if (!viewer || !api?.Three || !api?.dispatchEvent) return;
    const settingsURL = new URL("settings.json", document.currentScript.src);
    let config = null;
    let active = null;
    let disposed = false;
    let fetching = false;
    let warning = false;
    const metrics = {active: false, tiles: 0, geometryBytes: 0, radius: 0, reason: "disabled"};
    const diagnostic = message => {
        if (!warning) console.warn("Create Sky BlueMap: " + message);
        warning = true;
    };

    function detach() {
        if (!active) return;
        const state = active;
        active = null;
        viewer.render = state.render;
        viewer.updateLoadedMapArea = state.update;
        state.manager.tryLoadTile = state.tryLoad;
        state.manager.loadCloseTiles = state.loadClose;
        state.manager.handleLoadedTile = state.loaded;
        for (const [manager, loaded] of state.coarseCallbacks) {
            manager.handleLoadedTile = loaded;
            // Map.unload clears its manager properties before the map-changed event.
            // Retain the captured managers so teardown also works after that unload.
            manager.removeAllTiles();
        }
        for (const [loader, shader] of state.shaders) loader.fragmentShader = shader;
        state.manager.removeAllTiles();
        Object.assign(metrics, {active: false, tiles: 0, geometryBytes: 0, radius: 0, reason: "disabled"});
        if (viewer.map === state.map && state.map.isLoaded) viewer.updateLoadedMapArea();
    }

    function measure(manager) {
        const arrays = new Set();
        manager.scene.traverse(object => {
            const geometry = object.geometry;
            if (!geometry) return;
            for (const attribute of Object.values(geometry.attributes)) {
                if (attribute.array) arrays.add(attribute.array.buffer);
                else if (attribute.data?.array) arrays.add(attribute.data.array.buffer);
            }
            if (geometry.index?.array) arrays.add(geometry.index.array.buffer);
        });
        return [...arrays].reduce((total, buffer) => total + buffer.byteLength, 0);
    }

    // Lowres stores color/occupancy above signed height metadata. Never use height as occupancy.
    // This removes triangles touching empty samples; it does not reconstruct missing 3D surfaces.
    function clipVoid(mesh) {
        if (!mesh?.isMesh || mesh.userData.tileType !== "lowres" || mesh.userData.createSkyGeometry) return;
        // Coarser grids can have no fully occupied triangle even for a whole island.
        // Retain upstream geometry there rather than silently erasing those islands.
        if (mesh.material.uniforms.lod.value !== 1) return;
        const texture = mesh.material.uniforms.textureImage.value;
        const canvas = document.createElement("canvas");
        canvas.width = texture.image.width;
        canvas.height = texture.image.height;
        const context = canvas.getContext("2d", {willReadFrequently: true});
        context.drawImage(texture.image, 0, 0);
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
        const original = mesh.geometry;
        const positions = original.attributes.position;
        const indices = original.index.array;
        const occupied = new Uint8Array(positions.count);
        for (let i = 0; i < positions.count; i++) {
            const x = Math.max(0, Math.min(canvas.width - 1, Math.floor(positions.getX(i))));
            const z = Math.max(0, Math.min(canvas.height / 2 - 1, Math.floor(positions.getZ(i))));
            occupied[i] = pixels[(z * canvas.width + x) * 4 + 3] > 0 ? 1 : 0;
        }
        const kept = [];
        for (let i = 0; i < indices.length; i += 3) {
            if (occupied[indices[i]] && occupied[indices[i + 1]] && occupied[indices[i + 2]]) {
                kept.push(indices[i], indices[i + 1], indices[i + 2]);
            }
        }
        if (kept.length === indices.length) return;
        mesh.geometry = original.clone();
        mesh.geometry.setIndex(kept);
        mesh.userData.createSkyGeometry = true;
        const ownedGeometry = mesh.geometry;
        // Upstream shares coarse geometry and only disposes its material. This clone is ours.
        mesh.material.addEventListener("dispose", () => ownedGeometry.dispose());
    }

    function attach() {
        const map = viewer.map;
        const eligible = config?.maps?.includes(map?.data?.id);
        if (!eligible || !map?.isLoaded) { detach(); return; }
        if (active?.map === map && active.config === config) return;
        detach();
        const script = [...document.scripts].some(element => {
            try { return new URL(element.src).pathname.endsWith("/" + config.bundle); }
            catch { return false; }
        });
        const manager = map.hiresTileManager;
        const needle = "distance < 1000.0 && texture(hiresTileMap.map";
        if (!script || !manager?.tiles || api.TileManager.tileMapSize !== 100
                || typeof manager.tryLoadTile !== "function" || typeof manager.handleLoadedTile !== "function"
                || !map.lowresTileManager.every(m => m.tileLoader.fragmentShader.split(needle).length === 2)) {
            metrics.reason = "unsupported webapp";
            diagnostic("unsupported viewer internals; using the original renderer");
            return;
        }
        const state = {map, manager, config, render: viewer.render, update: viewer.updateLoadedMapArea,
            tryLoad: manager.tryLoadTile, loadClose: manager.loadCloseTiles, loaded: manager.handleLoadedTile,
            shaders: [], coarseCallbacks: [], bounds: config.bounds[map.data.id], radius: config.tileRadius, frustum: new api.Three.Frustum(),
            matrix: new api.Three.Matrix4(), box: new api.Three.Box3(), lastMeasure: 0};
        active = state;
        metrics.active = true;
        metrics.reason = "detail";
        const rewrite = shader => shader.replace(needle, `distance < ${config.detailDistance.toFixed(1)} && texture(hiresTileMap.map`);
        for (const lowres of map.lowresTileManager) {
            state.shaders.push([lowres.tileLoader, lowres.tileLoader.fragmentShader]);
            lowres.tileLoader.fragmentShader = rewrite(lowres.tileLoader.fragmentShader);
            const loaded = lowres.handleLoadedTile;
            state.coarseCallbacks.push([lowres, loaded]);
            lowres.handleLoadedTile = tile => {
                if (active === state) {
                    try { clipVoid(tile.model); }
                    catch (error) { diagnostic("coarse occupancy unavailable; retaining the original tile: " + error.message); }
                }
                loaded(tile);
            };
            lowres.scene.traverse(mesh => {
                try { clipVoid(mesh); }
                catch (error) { diagnostic("coarse occupancy unavailable: " + error.message); }
            });
        }
        function materials() {
            for (const lowres of map.lowresTileManager) lowres.scene.traverse(object => {
                const material = object.material;
                if (material?.fragmentShader?.includes(needle) && material.userData.createSkyShader === undefined) {
                    material.userData.createSkyShader = material.fragmentShader;
                    material.fragmentShader = rewrite(material.fragmentShader);
                    material.needsUpdate = true;
                }
            });
        }
        manager.tryLoadTile = function(x, z) {
            if (this.currentlyLoading >= config.concurrentRequests || metrics.geometryBytes >= config.geometryMiB * 1048576) return false;
            const size = map.data.hires.tileSize;
            const offset = map.data.hires.translate;
            state.box.min.set(x * size.x + offset.x, state.bounds.minY, z * size.z + offset.z);
            state.box.max.set((x + 1) * size.x + offset.x, state.bounds.maxY, (z + 1) * size.z + offset.z);
            if (!state.frustum.intersectsBox(state.box)) return false;
            return state.tryLoad.call(this, x, z);
        };
        // Check the concurrency bound before each launch. Upstream checks it after launch.
        manager.loadCloseTiles = function() {
            if (active !== state || this.unloaded) return;
            if (this.loadTimeout) clearTimeout(this.loadTimeout);
            while (this.currentlyLoading < config.concurrentRequests && this.loadNextTile()) { /* bounded */ }
            if (this.currentlyLoading > 0) this.loadTimeout = setTimeout(this.loadCloseTiles, 200);
        }.bind(manager);
        manager.handleLoadedTile = function(tile) {
            state.loaded.call(this, tile);
            if (active !== state) return;
            metrics.geometryBytes = measure(this);
            if (metrics.geometryBytes > config.geometryMiB * 1048576) {
                state.radius = Math.max(1, state.radius - 1);
                metrics.radius = state.radius * Math.min(map.data.hires.tileSize.x, map.data.hires.tileSize.z);
                metrics.reason = "geometry budget";
                this.loadAroundTile(this.centerTile.x, this.centerTile.y, state.radius, state.radius);
                metrics.geometryBytes = measure(this);
                // An individual tile can exceed the entire budget; retain coarse fallback for it.
                if (metrics.geometryBytes > config.geometryMiB * 1048576) {
                    tile.unload();
                    metrics.geometryBytes = measure(this);
                }
            }
        }.bind(manager);
        viewer.updateLoadedMapArea = function() {
            if (active !== state || this.map !== map) return state.update.call(this);
            const originalFar = this.camera.far;
            this.camera.far = Math.max(originalFar, this.controlsManager.distance + 5000);
            this.camera.updateProjectionMatrix();
            this.camera.updateMatrixWorld();
            state.matrix.multiplyMatrices(this.camera.projectionMatrix, this.camera.matrixWorldInverse);
            state.frustum.setFromProjectionMatrix(state.matrix);
            this.camera.far = originalFar;
            this.camera.updateProjectionMatrix();
            const size = map.data.hires.tileSize;
            const nextX = Math.floor((this.data.loadedCenter.x - map.data.hires.translate.x) / size.x);
            const nextZ = Math.floor((this.data.loadedCenter.y - map.data.hires.translate.z) / size.z);
            if (Math.abs(nextX - manager.centerTile.x) > state.radius
                    || Math.abs(nextZ - manager.centerTile.y) > state.radius) state.radius = config.tileRadius;
            const distance = this.controlsManager.distance < config.detailDistance
                ? state.radius * Math.min(size.x, size.z) : 0;
            map.loadMapArea(this.data.loadedCenter.x, this.data.loadedCenter.y, distance, this.data.loadedLowresViewDistance);
            metrics.radius = distance;
            metrics.reason = distance ? (state.radius < config.tileRadius ? "geometry budget" : "detail") : "distance fallback";
        }.bind(viewer);
        viewer.render = function(delta) {
            if (active !== state || this.map !== map) return state.render.call(this, delta);
            metrics.tiles = manager.tiles.size;
            if (performance.now() - state.lastMeasure > 1000) {
                metrics.geometryBytes = measure(manager);
                state.lastMeasure = performance.now();
            }
            renderDetail.call(this, delta, config.detailDistance);
        };
        materials();
        viewer.updateLoadedMapArea();
    }

    async function refresh() {
        if (disposed || fetching) return;
        fetching = true;
        try {
            const response = await fetch(settingsURL, {cache: "no-store"});
            if (!response.ok) throw new Error("adapter settings unavailable");
            const next = await response.json();
            if (next.schema !== 2 || !Array.isArray(next.maps)) throw new Error("unsupported settings schema");
            if (next.maps.length && (!Number.isInteger(next.detailDistance) || next.detailDistance < 1000 || next.detailDistance > 8000
                    || !Number.isInteger(next.tileRadius) || next.tileRadius < 1 || next.tileRadius > 48
                    || !Number.isInteger(next.concurrentRequests) || next.concurrentRequests < 1 || next.concurrentRequests > 8
                    || !Number.isInteger(next.geometryMiB) || next.geometryMiB < 32 || next.geometryMiB > 1024)) {
                throw new Error("invalid resource bounds");
            }
            if (next.maps.some(id => {
                const bounds = next.bounds?.[id];
                return !bounds || !Number.isInteger(bounds.minY) || !Number.isInteger(bounds.maxY)
                    || bounds.minY >= bounds.maxY || bounds.maxY - bounds.minY > 4096;
            })) throw new Error("invalid world height bounds");
            if (disposed) return;
            if (JSON.stringify(next) !== JSON.stringify(config)) config = next;
            attach();
        } catch (error) {
            detach();
            diagnostic(error.message);
        } finally { fetching = false; }
    }
    const changed = () => { attach(); refresh(); };
    const moved = () => { if (active) viewer.updateLoadedMapArea(); };
    app.events.addEventListener("bluemapMapChanged", changed);
    app.events.addEventListener("bluemapCameraMoved", moved);
    const timer = setInterval(refresh, 30000);
    window.createSkyBlueMap = {
        metrics,
        refresh,
        dispose() {
            disposed = true;
            clearInterval(timer);
            app.events.removeEventListener("bluemapMapChanged", changed);
            app.events.removeEventListener("bluemapCameraMoved", moved);
            detach();
        }
    };
    refresh();
})();
