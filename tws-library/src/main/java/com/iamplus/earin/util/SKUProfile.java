package com.iamplus.earin.util;

public class SKUProfile {

    public static String [] colorVersions = {
            "Invalid",
            "Earin Black",
            "Earin White",
            "Omega Black",
            "Omega White"
    };
    public static String [] skuVersion = {
            "Invalid",
            "NBB",
            "NWS",
            "OBB",
            "OWS",
            "EBB",
            "EWS"
    };

    public static String [] earinVersions = {
            "Invalid SKU",
            "None Omega version in black mechanics",
            "None OMEGA version in white mechanics",
            "Omega version in black mechanics",
            "Omega version in white mechanics",
            "Earin version in black mechanics",
            "Earin version in white mechanics"
    };

    public static String getColorVersion(String sku) {
        return colorVersions[getColorVersionInt(sku) - 1];
    }

    public static int getColorVersionInt(String sku) {
        if(validateSKU(sku)) {
            return ((int)sku.charAt(3));
        }
        return 1;
    }

    public static boolean validateSKU(String sku) {
        boolean isSKUValid = false;
        if(sku.length() == 4) {
            isSKUValid = true;
        }
        return isSKUValid;
    }

    public static String getEarinVersion(String mSku) {
        String sku = mSku.substring(2,mSku.length());
        switch (sku) {
            case "22": return earinVersions[5];
            case "32": return earinVersions[6];
            case "41": return earinVersions[3];
            case "40": return earinVersions[1];
            case "50": return earinVersions[2];
            case "51": return earinVersions[4];
            default:return earinVersions[0];
        }
    }

    public static String getSKUVersion(String mSku) {
        String sku = mSku.substring(2,mSku.length());
        switch (sku) {
            case "22": return skuVersion[5];
            case "32": return skuVersion[6];
            case "41": return skuVersion[3];
            case "40": return skuVersion[1];
            case "50": return skuVersion[2];
            case "51": return skuVersion[4];
            default:return skuVersion[0];
        }
    }
}
