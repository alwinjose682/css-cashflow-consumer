package io.alw.css.cashflowconsumer.repository.mapper;

import io.alw.css.domain.common.YesNo;

class MapperUtil {
    public static YesNo booleanToYesNo(boolean b) {
        return b ? YesNo.Y : YesNo.N;
    }

    public static boolean yesNoToBoolean(YesNo yesNo) {
        return yesNo == YesNo.Y;
    }
}
