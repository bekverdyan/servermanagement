{
	"gitUserName": "sergeyhlghatyan-scdm",
	"gitPassword": "RErPq2k4",
	"snapshot": "nabs-qa",
	"ec2": [{
		"id": "i-d2e7de6e",
		"name": "NABS-QA-41",
		"rds": "NABS-QA-41",
		"replaceFiles": [{
			"file": "/home/ubuntu/code/nabs/NABS/NABS-java/src/main/resources/settings.properties",
			"keyWords": [{
				"key": "db_username",
				"value": "=nabs"
			}, {
				"key": "sid",
				"value": "=NABS"
			},
			 {
				"key": "db_password",
				"value": "=nihenhucy"
			},
			{
				"key": "db_connection_url",
				"value": "="
			}, {
				"key": "temp_folder",
				"value": "=/home/ubuntu/temp"
			}, {
				"key": "current_user",
				"value": "=NABS-QA-41"
			}, {
				"key": "nabs_log_file_path",
				"value": "=/home/ubuntu/temp/nabs.log"
			}, {
				"key": "nabs_app_url",
				"value": "=http://localhost:80/NABSWeb"
			}]
		}, {
			"file": "/home/ubuntu/code/nabs/NABS/NABS-mainservice/src/main/resources/settings.properties",
			"keyWords": [{
				"key": "nabs_app_url",
				"value": "=http://localhost:80/NABSWeb"
			}, {
				"key": "main_mockup_log_file_path",
				"value": "=/home/ubuntu/temp/main.log"
			}]
		}, {
			"file": "/home/ubuntu/code/nabs/NABS/NABS-nabsservice/src/main/resources/settings.properties",
			"keyWords": [{
				"key": "nabs_app_url",
				"value": "=http://localhost:80/NABSWeb"
			}]
		}, {
			"file": "/home/ubuntu/code/grid/calibration/deployongrid.sh",
			"keyWords": [{
				"key": "soamreg",
				"value": " /home/ubuntu/code/grid/calibration/deployment.xml -f"
			}, {
				"key": "soamdeploy",
				"value": " add CalibrationService -p /home/ubuntu/code/grid/calibration/CalibrationService.zip -a /home/ubuntu/code/grid/calibration/deployment.xml -f"
			}]
		}, {
			"file": "/home/ubuntu/code/grid/calibration/src/main/resources/calibrationservice.properties",
			"keyWords": [{
				"key": "gridside_log_file_path",
				"value": "=/home/ubuntu/temp/calibration_service.log"
			}]
		}],
		"branches": {
			"nabs": "release_20161128",
			"foundation": "11.0",
			"finval": "2.3.21",
			"economic-meaning": "release_20161128",
			"grid": "release_20161128",
			"finmath-adapter": "finmath-finval-adapter-0.4.9"
		}
	}]
}