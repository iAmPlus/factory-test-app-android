/*****************************************************************************

            (c) Cambridge Silicon Radio Limited 2010
            All rights reserved and confidential information of CSR

            Refer to LICENSE.txt included with this source for details
            on the license terms.

*****************************************************************************/
#include "csr_synergy.h"

#include <stdlib.h>
#include <string.h>
#include "csr_pmem.h"

#define NULL 0

/*----------------------------------------------------------------------------*
 *  NAME
 *      CsrPmemInit
 *
 *  DESCRIPTION
 *      Sets up the pool control blocks and establishes the pools' free
 *      lists.   Use only at the system's initialisation.
 *
 *  RETURNS
 *      void
 *
 *----------------------------------------------------------------------------*/
void CsrPmemInit(void)
{
}

void CsrPmemDeinit(void)
{
}

/*----------------------------------------------------------------------------*
 *  NAME
 *      CsrPmemAlloc
 *
 *  DESCRIPTION
 *      Returns a pointer to a block of memory of length "size" bytes obtained
 *      from the pools.
 *
 *      Panics on failure.
 *
 *  RETURNS
 *      void * - pointer to allocated block
 *
 *----------------------------------------------------------------------------*/

void *CsrPmemAlloc(CsrSize size)
{
    return malloc(size);
}

/*----------------------------------------------------------------------------*
 *  NAME
 *      CsrPmemFree
 *
 *  DESCRIPTION
 *      Return a memory block previously obtained via CsrPmemAlloc to the pools.
 *
 *  RETURNS
 *      void
 *
 *----------------------------------------------------------------------------*/
void CsrPmemFree(void *ptr)
{
    if (ptr == NULL)
    {
        return;
    }

    free(ptr);
}
