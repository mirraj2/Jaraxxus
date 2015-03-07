#!/bin/sh

keytool -import -v -trustcacerts -alias root -file AddTrustExternalCARoot.crt -keystore keyStore.jks
keytool -import -v -trustcacerts -alias intermed-2 -file COMODORSAAddTrustCA.crt -keystore keyStore.jks
keytool -import -v -trustcacerts -alias intermed-1 -file COMODORSADomainValidationSecureServerCA.crt -keystore keyStore.jks
keytool -import -v -trustcacerts -alias server -file jaraxxus_com.crt -keystore keyStore.jks

