# Create Sky Compat 0.17.0

Add an opt-in Macaw’s Bridges 3.1.2 adaptation for the matching short-chain hanging stair-lantern resource model.

- Match the selection outline to the hanging model in all four directions.
- Select the portion extending outside its owning cell without extending reach or bypassing native obstructions.
- Preserve flat lanterns, empty physical collision and native placement/removal behavior.

Enable `macaw.bridge_lantern_shape` on client and server only with the matching resource model. The standalone default is off. The configuration screen groups it under Server / shared → Macaw’s Bridges.

No model or texture assets are bundled in Compat. Create Sky supplies the resource model separately. Existing terrain, rendering and gameplay adaptations are unchanged.
