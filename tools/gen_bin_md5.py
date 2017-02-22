import os
import sys
from hashlib import md5

def showError(msg):
    print msg
    os.system('pause')
    exit()

if len(sys.argv) != 2:
    showError('Invalid params')

BIN_PREFIX = "CaptureCamera_"

apkPath = sys.argv[1]
print apkPath
apkDir = os.path.dirname(apkPath)
#get ver code
verCode = raw_input('verCode:')
#gen md5 value
apkFile = open(apkPath,'rb')
m = md5()
m.update(apkFile.read())
apkFile.close()
md5Value = m.hexdigest()
#rename
apkCustName = BIN_PREFIX+verCode+'_'+md5Value+'.bin'
os.rename(apkPath,os.path.join(apkDir,apkCustName))

os.system('pause')
