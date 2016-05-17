== Openstack on clouddriver

=== Developing clouddriver-openstack

Need to run clouddriver locally for development? Here's what you need to setup and run:

## Environment Setup
. git clone git@github.com:spinnaker/clouddriver.git
. mkdir ~/.spinnaker
. cd ~/.spinnaker
. ln -s /path/to/clouddriver/clouddriver-web/config/clouddriver.yml
. cd /path/to/clouddriver

## Docker Setup
. docker-machine create --virtualbox-disk-size 8192 --virtualbox-memory 4096 -d virtualbox clouddriver-instance
. eval $(docker-machine env clouddriver-instance)
. docker-compose -f ./clouddriver-openstack/docker-compose.yml up -d

## Running App
. ./gradlew bootRun

## Verifying
. curl -v localhost:7002/applications

## Swagger
. http://localhost:7002/swagger-ui.html
