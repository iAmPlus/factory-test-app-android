#ifndef _CSR_SYNERGY_H
#define _CSR_SYNERGY_H
/****************************************************************************
 *
 *       (c) Cambridge Silicon Radio Limited 2011
 *
 *       All rights reserved and confidential information of CSR
 *
 ****************************************************************************/
#include "csr_components.h"

#define CSR_PLATFORM_LINUX

#define CSR_FUNCATTR_NORETURN(x) x __attribute__ ((noreturn))

/* Versioning */
#define CSR_VERSION_NUMBER(major,minor,fix) ((major * 10000) + (minor * 100) + fix)

/* Overall configuration */
#ifndef CSR_ENABLE_SHUTDOWN
#define CSR_ENABLE_SHUTDOWN
#endif

#ifndef CSR_EXCEPTION_HANDLER
#define CSR_EXCEPTION_HANDLER
#endif

#ifndef CSR_EXCEPTION_PANIC
/* #undef CSR_EXCEPTION_PANIC */
#endif

#ifndef CSR_CHIP_MANAGER_TEST_ENABLE
/* #undef CSR_CHIP_MANAGER_TEST_ENABLE */
#endif

#ifndef CSR_CHIP_MANAGER_QUERY_ENABLE
/* #undef CSR_CHIP_MANAGER_QUERY_ENABLE */
#endif

#ifndef CSR_CHIP_MANAGER_ENABLE
/* #undef CSR_CHIP_MANAGER_ENABLE */
#endif

#ifndef CSR_BUILD_DEBUG
/* #undef CSR_BUILD_DEBUG */
#endif

#ifndef CSR_INSTRUMENTED_PROFILING_SERVICE
/* #undef CSR_INSTRUMENTED_PROFILING_SERVICE */
#endif

#ifndef CSR_LOG_ENABLE
/* #undef CSR_LOG_ENABLE */
#endif

#ifndef CSR_AMP_ENABLE
/* #undef CSR_AMP_ENABLE */
#endif

#ifndef CSR_BT_LE_ENABLE
/* #undef CSR_BT_LE_ENABLE */
#endif

/*#define CSR_FRW_BUILDSYSTEM_AVAILABLE
#ifdef CSR_FRW_BUILDSYSTEM_AVAILABLE
#include "csr_frw_config.h"
#endif*/

/*#ifdef CSR_COMPONENT_BT
#include "csr_bt_config.h"
#endif*/

/* #undef CSR_WIFI_BUILDSYSTEM_AVAILABLE */
/*#ifdef CSR_WIFI_BUILDSYSTEM_AVAILABLE
#include "csr_wifi_config.h"
#endif*/

/* #undef CSR_MERCURY_BUILDSYSTEM_AVAILABLE */
/*#ifdef CSR_MERCURY_BUILDSYSTEM_AVAILABLE
#include "csr_mercury_config.h"
#endif*/

/* Do not edit this area - Start */
#ifdef CSR_LOG_ENABLE
#ifndef CSR_LOG_INCLUDE_FILE_NAME_AND_LINE_NUMBER
#define CSR_LOG_INCLUDE_FILE_NAME_AND_LINE_NUMBER
#endif
#endif

#ifdef CSR_BUILD_DEBUG
#ifndef MBLK_DEBUG
#define MBLK_DEBUG
#endif
#endif

#ifdef CSR_ENABLE_SHUTDOWN
#ifndef ENABLE_SHUTDOWN
#define ENABLE_SHUTDOWN
#endif
#endif

#ifndef CSR_EXCEPTION_HANDLER
#ifndef EXCLUDE_CSR_EXCEPTION_HANDLER_MODULE
#define EXCLUDE_CSR_EXCEPTION_HANDLER_MODULE
#endif
#endif

#ifdef CSR_EXCEPTION_PANIC
#ifndef EXCEPTION_PANIC
#define EXCEPTION_PANIC
#endif
#endif

#ifndef FTS_VER
#define FTS_VER "9.9.19.0"
#endif

/* Do not edit this area - End */

void CsrSupressEmptyCompilationUnit(void);

#endif /* _CSR_SYNERGY_H */
