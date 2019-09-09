<!-- <wizard> -->
| [&laquo; Back: NB_IoT node](../../src/NB-IoT_node/README.md) | [HOME](/README.md) |
| :----------- | :-----------: |
<!-- <\wizard> -->
# Index
 * [Registering a Device on Cumulocity](#registering-a-device-on-cumulocity)
	 * [Requirements](#requirements)
	 * [Registration Steps](#registration-steps)
  
# Registering a Device on Cumulocity

## Requirements: 

Before you can register devices on Cumulocity, the steps below have to be performed successfully:

* [Setup a working cumulocity tenant](../cumulocityTenant/README.md)
* [Setup a running rabbitmq instance](../aws/ecs/rabbitmq-broker/README.md)
* [Setup a running agent instance](../aws/ecs/nb-iot-agent/README.md)
* [Setup a running node](../../src/NB-IoT_node/README.md)
  
[:top:](#)

## Registration steps:
1. On your Cumulocity tenant page, go to "Device management"  
	![cumulocity-setup](../../docs/img/cumulocity-setup-5.png)
2. Click on "Register device"  
	![cumulocity-setup](../../docs/img/cumulocity-setup-6.png)
3. Click on "General Device Registration"  
	![cumulocity-setup](../../docs/img/cumulocity-setup-7.png)
4. Enter the device IMEI (can be found on a sticker on the SARA-N211 module) and click "Next" and "Complete"  
	![cumulocity-setup](../../docs/img/cumulocity-setup-8.png)
5. You'll see your device "WAITING FOR CONNECTION"  
	![cumulocity-setup](../../docs/img/cumulocity-setup-9.png)
6. Wait until the communication with the device is successfull "PENDING ACCEPTANCE" and click the "accept" button  
	![cumulocity-setup](../../docs/img/cumulocity-setup-10.png)
7. Click on "All Devices" under "Devices"  
	![cumulocity-setup](../../docs/img/cumulocity-setup-11.png)
8. Click on your newly created device (device_ + number part)  
	![cumulocity-setup](../../docs/img/cumulocity-setup-12.png)
9. You can now view the device measurements!  
	![cumulocity-setup](../../docs/img/cumulocity-setup-13.png)
  
[:top:](#)
<!-- <wizard> -->
| [&laquo; Back: NB_IoT node](../../src/NB-IoT_node/README.md) | [HOME](/README.md) |
| :----------- | :-----------: |
<!-- <\wizard> -->
