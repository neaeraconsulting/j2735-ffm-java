# J2735 (2024) Dynamic Library

Built and tested with `asn1c`, [mouse07410/vlm_master branch](https://github.com/mouse07410/asn1c), on Ubuntu Linux 24.04.

ASN.1 files from SAE: [Download J2735 (2024) ASN.1 files from SAE](https://www.sae.org/standards/content/j2735set_202409/)

### Compile ASN.1 into a Dynamic Library

Compile the auto-generated converter example to create all the *.o files, and to make sure the generated C code is good.  
Assuming all the fixed asn.1 files are in an `asn` directory.  See the `hacks` directory for the notes on editing the 
ASN.1 and generated C files to be able to compile them.
```bash
cd 2024
asn1c -fcompound-names -fincludes-quoted -pdu=all asn/*.asn
cp hacks/*.* .
make -f converter-example.mk
```

### Create Dynamic Library
Compile a dynamic library for Java code to link to.

Add `-fPIC` (position independent code) flag to `converter-example.mk`:
```makefile
CFLAGS += $(ASN_MODULE_CFLAGS) -DASN_PDU_COLLECTION -I. -fPIC
```
Recompile:
```bash
rm *.o
make -f converter-example.mk
```
Build dynamic library:
```bash
gcc -shared -o libasnapplication.so *.o
```



