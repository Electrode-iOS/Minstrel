function valueToBridgeString(obj) {

    var rtn;
    switch (typeof obj) {
        case 'object':
            if (!obj) {
                rtn = JSON.stringify(obj);
            } else if (Array.isArray(obj)) {
                rtn = '[' + obj.map(function(item) {
                    return valueToBridgeString(item);
                }).join(',') + ']';
            } else {
                var rtn = '{';
                for (var name in obj) {
                    if (obj.hasOwnProperty(name)) {
                        if (rtn.length > 1) {
                            rtn += ',';
                        }
                        rtn += JSON.stringify(name);
                        rtn += ': ';
                        rtn += valueToBridgeString(obj[name]);
                    }
                }
                rtn += '}';
            }
        break;
        case 'function':
            rtn = 'function:' + __functionIDCounter.toString() + ':' + btoa(obj.toString());
            __functionCache[__functionIDCounter] = obj;
            __functionIDCounter++;
            if (__functionIDCounter > __functionIDLimit) { __functionIDCounter = 0; }
            rtn = JSON.stringify(rtn);
        break;
        default:
            if (obj === undefined) {
                rtn = 'null';
            } else {
                rtn = JSON.stringify(obj);
            }
    }

    return rtn;
}
