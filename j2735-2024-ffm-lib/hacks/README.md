### Hacks needed to get asn1c to compile the J2735 (2024) ASN.1 files

#### 1)

Edit `J2945-3-RoadWeatherMessage-2024-rel-v2.1.asn` per the issue described here:

[github: VLM asn1c, Issue #420](https://github.com/vlm/asn1c/issues/420)

Change the name of the `SnapShot` sequence in the `RoadWeatherMessage` module to `RwmSnapShot` so as not to conflict with `Snapshot` in the `ProbeVehicleData` module.

#### 2)

Edit `J3217-TollUsageMsg-2024-rel-v1.1.asn`:  Change the the name of the `VehicleId` sequence in the `TollUsageMessage` module to `TumVehicleId` so it doesn't conflict with `VehicleID` in the `ProbeVehicleData` module.

Also rename the imported type in `J3217-R-RoadUserChargingReportMsg-2024-rel-v1.1.asn`

Similar upper/lower case issue as above which manifests as error:

```
make: *** No rule to make target 'VehicleID.o', needed by 'libasncodec.a'.  Stop.
```



#### 3)

Edit the generated header file `NodeOffsetPointXY.h` to avoid the circular header include issue described here:

[github: USDOT asn1c, Issue #1](https://github.com/usdot-fhwa-stol/usdot-asn1c/issues/1)

#### 4)

Add the header:

```c
#include "/usr/include/time.h"
```

to the following files:

* GeneralizedTime.c
* GeneralizedTime_ber.c
* GeneralizedTime_xer.c
* GeneralizedTime_print.c
* GeneralizedTime_jer.c

There is a flag to point to that header location for CYGWIN, but it doesn't get set on Ubuntu.