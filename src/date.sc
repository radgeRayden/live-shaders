vvv bind time
do
    let header = (include "time.h")
    using header.extern
    using header.struct
    using header.define
    unlet header
    locals;

using import struct
struct Date plain
    year : i32
    month : i32
    day : i32
    second : f32

    inline __unpack (self)
        _ self.year self.month self.day self.second

fn get-date ()
    local ts : time.timespec
    assert ((time.clock_gettime time.CLOCK_REALTIME &ts) == 0)
    let cdate = (time.localtime &ts.tv_sec)

    # calculate seconds since midnight
    isec := (cdate.tm_hour * 3600) + (cdate.tm_min * 60) + cdate.tm_sec
    fsec := (isec as f32) + (ts.tv_nsec / 1000000000)

    Date
        year = (1900 + cdate.tm_year)
        month = cdate.tm_mon
        day = cdate.tm_mday
        second = fsec

do
    let get-date Date
    locals;