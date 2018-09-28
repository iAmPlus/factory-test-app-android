###############################################################################
#
#                (c) Copyright Cambridge Silicon Radio Limited and its licensors 2011-2012
#
#                All rights reserved and confidential information of CSR
#
###############################################################################
cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

file(GLOB sbc_src   "sbc/*.c")
file(GLOB sbc_inc   "sbc/*.h")
file(GLOB core_inc  "inc/*.h")
file(GLOB ports_src	"ports/*.c")

add_definitions(-DCSR_USE_STDC_LIB)

add_library(SbcLib
             STATIC
             # Associated headers in the same location as their source
             # file are automatically included.
             ${sbc_src}
             ${ports_src}
             )

add_library(SbcLibWrapper
             SHARED
             # Associated headers in the same location as their source
             # file are automatically included.
             "native-lib.cpp"
             )

include_directories( "inc" "ports" )

target_link_libraries(SbcLibWrapper
                      SbcLib
                      log
                      android)
