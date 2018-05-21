mvn gpg:sign-and-deploy-file -Dfile=./libuvccamera-release.aar -DpomFile=libuvccamera-pom.xml  -DrepositoryId=oss -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/
mvn gpg:sign-and-deploy-file -Dfile=./usbCameraCommon-release.aar -DpomFile=usbCameraCommon-pom.xml  -DrepositoryId=oss -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/
rm *.asc