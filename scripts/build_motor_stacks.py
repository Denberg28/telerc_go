#!/usr/bin/env python3
"""Build and package separate, self-contained MX1508 motor firmware stacks."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent.parent
FQBN = 'esp32:esp32:esp32s3:CDCOnBoot=cdc,USBMode=hwcdc,FlashSize=4M,PartitionScheme=default,PSRAM=disabled,FlashMode=dio'
CORE = '3.3.2'
PROFILES = {
    '4wd': {'sketch':'TeleRC_Go_4WD','tests':'go_4wd_tests','version':'1.3.0',
            'archive':'TeleRC-Go-4WD-v1.3.0-Candidate.zip',
            'layout':'Four-motor differential/skid steering',
            'pins':{'left_front':[4,5],'left_rear':[6,7],'right_front':[8,9],'right_rear':[10,11]}},
    '2motor': {'sketch':'TeleRC_Go_SpringCar','tests':'spring_car_tests','version':'0.2.0',
               'archive':'TeleRC-Go-SpringCar-v0.2.0-Candidate.zip',
               'layout':'Rear drive motor + front spring-return steering motor (estimated position)',
               'pins':{'rear_drive':[4,5],'front_steering':[8,9]}},
}
OFFSETS = {'bootloader':0,'partitions':0x8000,'boot_app0':0xe000,'application':0x10000}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_archive(archive, expected_commit=None):
    with zipfile.ZipFile(archive) as bundle:
        if bundle.testzip():
            raise ValueError('Invalid ZIP CRC')
        names = set(bundle.namelist())
        if len(names)!=len(bundle.namelist()) or any('merged' in n for n in names):
            raise ValueError('Duplicate or merged image')
        manifest = json.loads(bundle.read('MANIFEST.json'))
        if expected_commit and manifest['source_commit']!=expected_commit:
            raise ValueError('Source commit mismatch')
        profile=PROFILES.get(manifest.get('profile'))
        if not profile or manifest.get('pins')!=profile['pins'] or manifest.get('version')!=profile['version']:
            raise ValueError('Layout/profile mismatch')
        images = manifest['images']
        if {i['kind'] for i in images}!=set(OFFSETS) or len(images)!=4:
            raise ValueError('Incomplete image set')
        for image in images:
            if image['offset']!=hex(OFFSETS[image['kind']]) or image['path']!=f"firmware/{image['kind']}.bin":
                raise ValueError('Incorrect flash address or path')
            data = bundle.read(image['path'])
            if len(data)!=image['bytes'] or hashlib.sha256(data).hexdigest()!=image['sha256']:
                raise ValueError('Image digest mismatch')
        for item in manifest['source_files']:
            if hashlib.sha256(bundle.read(item['path'])).hexdigest()!=item['sha256']:
                raise ValueError('Source digest mismatch')
        sums = bundle.read('SHA256SUMS.txt').decode().splitlines()
        checked = set()
        for line in sums:
            sha,name=line.split('  ',1)
            if name in checked or hashlib.sha256(bundle.read(name)).hexdigest()!=sha:
                raise ValueError('Member checksum mismatch')
            checked.add(name)
        if checked!=names-{'SHA256SUMS.txt'}:
            raise ValueError('Unlisted archive member')
    return manifest


def package(profile_name, build, destination, commit, core_path, root=ROOT):
    profile=PROFILES[profile_name]
    images=[]
    # Validate completed build before creating an output archive.
    paths={kind:build/f"{profile['sketch']}.ino{suffix}" for kind,suffix in
           [('bootloader','.bootloader.bin'),('partitions','.partitions.bin'),('application','.bin')]}
    paths['boot_app0']=core_path/'tools/partitions/boot_app0.bin'
    for path in paths.values():
        if not path.is_file() or not path.stat().st_size:
            raise ValueError('Missing firmware image: '+str(path))
    stage=destination/profile_name
    stage.mkdir(parents=True,exist_ok=False)
    (stage/'firmware').mkdir()
    for kind,offset in OFFSETS.items():
        target=stage/'firmware'/f'{kind}.bin';shutil.copyfile(paths[kind],target)
        images.append({'kind':kind,'path':target.relative_to(stage).as_posix(),
                       'offset':hex(offset),'bytes':target.stat().st_size,'sha256':digest(target)})
    for name in [profile['sketch'],profile['tests']]:
        shutil.copytree(root/'bridge'/name,stage/'bridge'/name,ignore=shutil.ignore_patterns('__pycache__'))
    (stage/'docs').mkdir()
    for name in ['FIRMWARE_STACKS.md','SPRING_CAR.md','FIRMWARE_STACKS_FLASH.md']:
        shutil.copy2(root/'docs'/name,stage/'docs'/name)
    (stage/'README.md').write_text(f"# {profile['layout']}\n\nThis archive is the **{profile_name}** build, version {profile['version']}.\n\n"+(root/'docs/FIRMWARE_STACKS.md').read_text())
    shutil.copy2(root/'docs/FIRMWARE_STACKS_FLASH.md',stage/'FLASH.md')
    shutil.copy2(root/'scripts/flash-motor-stack.py',stage/'flash.py')
    shutil.copy2(build/'BUILD.log',stage/'BUILD.log')
    source=[]
    for path in sorted((stage/'bridge').rglob('*')):
        if path.is_file():source.append({'path':path.relative_to(stage).as_posix(),'sha256':digest(path)})
    manifest={'product':'TeleRC Go motor stack','profile':profile_name,'version':profile['version'],
              'layout':profile['layout'],'pins':profile['pins'],'source_commit':commit,
              'fqbn':FQBN,'esp32_core':CORE,'driver':'MX1508','motor_rail_v':5,
              'app_change_required':False,'hardware_validated':False,
              'position_feedback':False,'steering_estimated':profile_name=='2motor',
              'images':images,'source_files':source}
    (stage/'MANIFEST.json').write_text(json.dumps(manifest,indent=2)+'\n')
    members=sorted(p for p in stage.rglob('*') if p.is_file())
    (stage/'SHA256SUMS.txt').write_text(''.join(f'{digest(p)}  {p.relative_to(stage).as_posix()}\n' for p in members))
    archive=destination/profile['archive']
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as bundle:
        for path in sorted(stage.rglob('*')):
            if path.is_file():bundle.write(path,path.relative_to(stage).as_posix())
    validate_archive(archive,commit)
    return archive


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--target',choices=['all',*PROFILES],default='all')
    parser.add_argument('--commit',required=True)
    parser.add_argument('--cli',default='arduino-cli')
    parser.add_argument('--core-path',type=Path,default=Path.home()/'.arduino15/packages/esp32/hardware/esp32'/CORE)
    args=parser.parse_args()
    if not re.fullmatch('[0-9a-f]{40}',args.commit):raise ValueError('Exact commit SHA required')
    actual=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    if actual!=args.commit:raise ValueError('Checkout does not match the requested source commit')
    subprocess.run(['git','diff','--exit-code','HEAD','--','bridge','scripts','docs'],cwd=ROOT,check=True)
    cores=subprocess.check_output([args.cli,'core','list'],text=True)
    if not re.search(r'^esp32:esp32\s+3\.3\.2\s+',cores,re.M):raise ValueError('Install esp32:esp32@3.3.2')
    if args.output.exists():raise ValueError('Use a fresh output directory')
    args.output.mkdir(parents=True)
    archives=[]
    for name in PROFILES if args.target=='all' else [args.target]:
        profile=PROFILES[name];build=args.output/(name+'-build');build.mkdir()
        command=[args.cli,'compile','--fqbn',FQBN,'--output-dir',str(build),str(ROOT/'bridge'/profile['sketch'])]
        with (build/'BUILD.log').open('w') as log:
            subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True)
        archives.append(package(name,build,args.output,args.commit,args.core_path))
    (args.output/'SHA256SUMS.txt').write_text(''.join(f'{digest(p)}  {p.name}\n' for p in archives))
    print('\n'.join(str(p) for p in archives))


if __name__=='__main__':
    main()
