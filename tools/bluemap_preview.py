#!/usr/bin/env python3
"""Serve saved BlueMap tiles with the candidate viewer, without changing the server."""
import argparse
import functools
import json
import shutil
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--web', type=Path, required=True)
    parser.add_argument('--map', default='world')
    parser.add_argument('--port', type=int, default=8101)
    parser.add_argument('--min-y', type=int, default=-128)
    parser.add_argument('--max-y', type=int, default=768)
    args = parser.parse_args()
    root = args.web.resolve()
    if not (root / 'settings.json').is_file() or args.min_y >= args.max_y:
        parser.error('Expected a BlueMap web root and a valid exclusive height range')
    script = 'create-sky-compat/viewer.js'
    config = dict(schema=2, maps=[args.map], bounds={args.map: dict(minY=args.min_y, maxY=args.max_y)},
                  bundle='assets/index-b72fc5a8.js', detailDistance=4000, tileRadius=31,
                  concurrentRequests=4, geometryMiB=256)

    class Handler(SimpleHTTPRequestHandler):
        def do_GET(self):
            try:
                self.serve()
            except (BrokenPipeError, ConnectionResetError):
                pass  # Browser navigation cancels in-flight requests.

        def serve(self):
            path = urlsplit(self.path).path
            data = None
            mime = 'application/json'
            if path == '/settings.json':
                settings = json.loads((root / 'settings.json').read_text())
                settings['scripts'] = list(dict.fromkeys([*settings.get('scripts', []), script]))
                data = json.dumps(settings).encode()
            elif path == '/create-sky-compat/settings.json':
                data = json.dumps(config).encode()
            elif path == '/' + script:
                data = (ROOT / 'src/main/resources/bluemap/viewer.js').read_bytes()
                mime = 'text/javascript'
            if data is not None:
                self.send_response(200)
                self.send_header('Content-Type', mime)
                self.send_header('Cache-Control', 'no-store')
                self.send_header('Content-Length', str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            else:
                selected = Path(self.translate_path(self.path)).resolve()
                if not selected.is_relative_to(root):
                    self.send_error(403)
                    return
                compressed = Path(str(selected) + '.gz')
                if not selected.exists() and compressed.is_file() and compressed.resolve().is_relative_to(root):
                    self.send_response(200)
                    self.send_header('Content-Type', self.guess_type(str(selected)))
                    self.send_header('Content-Encoding', 'gzip')
                    self.send_header('Content-Length', str(compressed.stat().st_size))
                    self.end_headers()
                    try:
                        with compressed.open('rb') as source:
                            shutil.copyfileobj(source, self.wfile)
                    except (BrokenPipeError, ConnectionResetError):
                        pass
                    return
                super().do_GET()

        def log_message(self, *_):
            pass

    print(f'Read-only BlueMap preview: http://127.0.0.1:{args.port}/', flush=True)
    with ThreadingHTTPServer(('127.0.0.1', args.port), functools.partial(Handler, directory=str(root))) as server:
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == '__main__':
    main()
