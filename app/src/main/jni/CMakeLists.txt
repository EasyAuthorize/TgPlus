cmake_minimum_required(VERSION 3.17)

project(TgPlus)

include_directories(openssl)

add_library( # lib name
			 TgPlus

			 # SHARED library
			 SHARED

			 # source file
			 TgPlus.c )

target_link_libraries(TgPlus
	#crypto
	#ssl
	log)
