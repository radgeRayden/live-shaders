using import String

let C = (import .radlib.libc)

fn read-file (filename)
    local buf : String
    do
        using C.stdio
        let fhandle = (fopen filename "rb")
        fseek fhandle 0 SEEK_END
        let flen = (ftell fhandle)
        fseek fhandle 0 SEEK_SET

        'resize buf (flen as usize)
        fread buf._items 1 (flen as u64) fhandle
        buf._items @ flen = 0:i8
        fclose fhandle
    buf

do
    let read-file
    locals;