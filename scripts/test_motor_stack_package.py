"""Prevent incomplete, corrupt or confused 4WD/SpringCar firmware deliveries."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
spec=importlib.util.spec_from_file_location('stacks',Path(__file__).with_name('build_motor_stacks.py'))
stacks=importlib.util.module_from_spec(spec);spec.loader.exec_module(stacks)

class StackPackageTests(unittest.TestCase):
    def fixture(self,base,name):
        profile=stacks.PROFILES[name];build=base/'build';build.mkdir()
        for suffix in ['.bin','.bootloader.bin','.partitions.bin','.merged.bin']:
            (build/(profile['sketch']+'.ino'+suffix)).write_bytes(b'test-image')
        (build/'BUILD.log').write_text('simulated fixture; not a board build\n')
        core=base/'core';(core/'tools/partitions').mkdir(parents=True)
        (core/'tools/partitions/boot_app0.bin').write_bytes(b'boot')
        return build,core
    def test_each_layout_has_exact_source_images_and_offsets(self):
        for name in stacks.PROFILES:
            with self.subTest(name=name),tempfile.TemporaryDirectory() as d:
                base=Path(d);build,core=self.fixture(base,name)
                archive=stacks.package(name,build,base/'dist','a'*40,core)
                manifest=stacks.validate_archive(archive,'a'*40)
                self.assertEqual(stacks.PROFILES[name]['pins'],manifest['pins'])
                with zipfile.ZipFile(archive) as z:
                    self.assertFalse(any('merged' in n for n in z.namelist()))
                    self.assertEqual(4,len([n for n in z.namelist() if n.startswith('firmware/')]))
                    for line in z.read('SHA256SUMS.txt').decode().splitlines():
                        sha,path=line.split('  ',1);self.assertEqual(sha,hashlib.sha256(z.read(path)).hexdigest())
                with self.assertRaises(ValueError):stacks.validate_archive(archive,'b'*40)
    def test_missing_application_prevents_archive(self):
        with tempfile.TemporaryDirectory() as d:
            base=Path(d);build,core=self.fixture(base,'4wd')
            (build/'TeleRC_Go_4WD.ino.bin').unlink()
            with self.assertRaises(ValueError):stacks.package('4wd',build,base/'dist','a'*40,core)
            self.assertFalse((base/'dist'/stacks.PROFILES['4wd']['archive']).exists())
    def test_corrupt_image_and_layout_are_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            base=Path(d);build,core=self.fixture(base,'4wd')
            archive=stacks.package('4wd',build,base/'dist','a'*40,core)
            for mode in ['image','layout','unlisted']:
                corrupt=base/(mode+'.zip')
                with zipfile.ZipFile(archive) as src,zipfile.ZipFile(corrupt,'w') as dst:
                    for name in src.namelist():
                        data=src.read(name)
                        if mode=='image' and name=='firmware/application.bin':data=b'corrupt'
                        if mode=='layout' and name=='MANIFEST.json':
                            m=json.loads(data);m['pins']={'rear_drive':[4,5]};data=json.dumps(m).encode()
                        dst.writestr(name,data)
                    if mode=='unlisted':dst.writestr('extra.bin',b'not in checksums')
                with self.assertRaises(ValueError):stacks.validate_archive(corrupt,'a'*40)
if __name__=='__main__':unittest.main()
