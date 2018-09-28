#ifndef CSR_UTIL_H__
#define CSR_UTIL_H__
/*****************************************************************************

        Copyright Cambridge Silicon Radio Limited 2012
        All rights reserved

        Refer to LICENSE.txt included with this source for details
        on the license terms.

*****************************************************************************/

#include "csr_synergy.h"
#include "csr_types.h"
#include "csr_macro.h"

#ifdef __cplusplus
extern "C" {
#endif

/*------------------------------------------------------------------*/
/* Bits - intended to operate on CsrUint32 values */
/*------------------------------------------------------------------*/
CsrUint8 CsrBitCountSparse(CsrUint32 n);
CsrUint8 CsrBitCountDense(CsrUint32 n);

/*------------------------------------------------------------------*/
/* Base conversion */
/*------------------------------------------------------------------*/
#define I2B10_MAX 12

CsrBool CsrHexStrToUint8(const CsrCharString *string, CsrUint8 *returnValue);
CsrBool CsrHexStrToUint16(const CsrCharString *string, CsrUint16 *returnValue);
CsrBool CsrHexStrToUint32(const CsrCharString *string, CsrUint32 *returnValue);
CsrUint32 CsrPow(CsrUint32 base, CsrUint32 exponent);
void CsrIntToBase10(CsrInt32 number, CsrCharString *str);
void CsrUInt16ToHex(CsrUint16 number, CsrCharString *str);
void CsrUInt32ToHex(CsrUint32 number, CsrCharString *str);

/*------------------------------------------------------------------*/
/* Standard C Library functions */
/*------------------------------------------------------------------*/
#ifdef CSR_USE_STDC_LIB
#define CsrMemCpy memcpy
#define CsrMemMove memmove
#define CsrStrCpy strcpy
#define CsrStrNCpy strncpy
#define CsrStrCat strcat
#define CsrStrNCat strncat
#define CsrMemCmp(s1, s2, n) ((CsrInt32) memcmp((s1), (s2), (n)))
#define CsrStrCmp(s1, s2) ((CsrInt32) strcmp((s1), (s2)))
#define CsrStrNCmp(s1, s2, n) ((CsrInt32) strncmp((s1), (s2), (n)))
/*#define CsrMemChr memchr*/
#define CsrStrChr strchr
/*#define CsrStrCSpn strcspn*/
/*#define CsrStrPBrk strpbrk*/
/*#define CsrStrRChr strrchr*/
/*#define CsrStrSpn strspn*/
#define CsrStrStr strstr
/*#define CsrStrTok strtok*/
#define CsrMemSet memset
#define CsrStrLen strlen
/*#define CsrVsnprintf(s, n, format, arg) ((CsrInt32) vsnprintf((s), (n), (format), (arg)))*/
#else /* !CSR_USE_STDC_LIB */
void *CsrMemCpy(void *dest, const void *src, CsrSize count);
void *CsrMemMove(void *dest, const void *src, CsrSize count);
CsrCharString *CsrStrCpy(CsrCharString *dest, const CsrCharString *src);
CsrCharString *CsrStrNCpy(CsrCharString *dest, const CsrCharString *src, CsrSize count);
CsrCharString *CsrStrCat(CsrCharString *dest, const CsrCharString *src);
CsrCharString *CsrStrNCat(CsrCharString *dest, const CsrCharString *src, CsrSize count);
CsrInt32 CsrMemCmp(const void *buf1, const void *buf2, CsrSize count);
CsrInt32 CsrStrCmp(const CsrCharString *string1, const CsrCharString *string2);
CsrInt32 CsrStrNCmp(const CsrCharString *string1, const CsrCharString *string2, CsrSize count);
CsrCharString *CsrStrChr(const CsrCharString *string, CsrCharString c);
CsrCharString *CsrStrStr(const CsrCharString *string1, const CsrCharString *string2);
void *CsrMemSet(void *dest, CsrUint8 c, CsrSize count);
CsrSize CsrStrLen(const CsrCharString *string);
#endif /* !CSR_USE_STDC_LIB */
CsrInt32 CsrVsnprintf(CsrCharString *string, CsrSize count, const CsrCharString *format, va_list args);

/*------------------------------------------------------------------*/
/* Non-standard utility functions */
/*------------------------------------------------------------------*/
#ifdef CSR_PMEM_DEBUG
void *CsrMemDupDebug(const void *buf1, CsrSize count, const CsrCharString *file, CsrUint32 line);
CsrCharString *CsrStrDupDebug(const CsrCharString *string, const CsrCharString *file, CsrUint32 line);
#define CsrMemDup(b, c) CsrMemDupDebug((b), (c), __FILE__, __LINE__)
#define CsrStrDup(str) CsrStrDupDebug((str), __FILE__, __LINE__)
#else
void *CsrMemDup(const void *buf1, CsrSize count);
CsrCharString *CsrStrDup(const CsrCharString *string);
#endif
int CsrStrNICmp(const CsrCharString *string1, const CsrCharString *string2, CsrSize count);
CsrUint32 CsrStrToInt(const CsrCharString *string);
CsrCharString *CsrStrNCpyZero(CsrCharString *dest, const CsrCharString *src, CsrSize count);

/*------------------------------------------------------------------*/
/* Filename */
/*------------------------------------------------------------------*/
const CsrCharString *CsrGetBaseName(const CsrCharString *file);

/*------------------------------------------------------------------*/
/* Misc */
/*------------------------------------------------------------------*/
CsrBool CsrIsSpace(CsrUint8 c);
#define CsrOffsetOf offsetof

#ifdef __cplusplus
}
#endif

#endif
