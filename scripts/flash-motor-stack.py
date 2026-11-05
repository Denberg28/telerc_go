#!/usr/bin/env python3
"""Verify exact TeleRC Go motor-stack images, then optionally flash an ESP32-S3 via esptool."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port',help='ESP32-S3 USB serial port, e.g. COM5 or /dev/ttyACM0')
    parser.add_argument('--verify-only',action='store_true')
    args=parser.parse_args()
    root=Path(__file__).resolve().parent
    manifest=json.loads((root/'MANIFEST.json').read_text())
    expected={'firmware/bootloader.bin':0,'firmware/partitions.bin':0x8000,
              'firmware/boot_app0.bin':0xe000,'firmware/application.bin':0x10000}
    images=manifest['images']
    if len(images)!=4 or {i['path'] for i in images}!=set(expected):
        raise ValueError('Unexpected firmware image list')
    operands=[]
    for item in sorted(images,key=lambda i:int(i['offset'],16)):
        if int(item['offset'],16)!=expected[item['path']]:
            raise ValueError('Unexpected firmware address')
        path=root/item['path']
        data=path.read_bytes()
        if len(data)!=item['bytes'] or hashlib.sha256(data).hexdigest()!=item['sha256']:
            raise ValueError('Firmware digest/size mismatch: '+item['path'])
        operands.extend([item['offset'],str(path)])
    print('Verified four ESP32-S3 images; source '+manifest['source_commit'])
    if args.verify_only:
        return 0
    if not args.port:
        parser.error('--port is required unless --verify-only is selected')
    print('Motor/battery power and motor leads must be disconnected during flashing.')
    return subprocess.call([sys.executable,'-m','esptool','--chip','esp32s3',
                            '--port',args.port,'--baud','460800','write-flash',*operands])
if __name__=='__main__':
    try:
        raise SystemExit(main())
    except (OSError,ValueError,KeyError) as error:
        print('Flash cancelled: '+str(error),file=sys.stderr)
        raise SystemExit(1)
