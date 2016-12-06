cd /opt/ibm/platformsymphonyde/de611/;
sudo su <<EOF
. profile.platform
soamshutdown &
sleep 8
soamstartup &
sleep 8
cd /home/ubuntu/code/grid/calibration/ 
./deployongrid.sh 
/etc/init.d/tomcat6 restart
EOF