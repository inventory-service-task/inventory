= Build

* After the cloning repository, go to the folder with the just cloned project
* Run build command: 
```bash
mvn clean install
```
* run docker image build:
```bash
docker build -t inventory-service:latest .
```
* go to .infra folder
```bash
cd .infra
```
* run docker-compose:
```bash
docker-compose up -d
```
the above command will run mongodb and spring boot app on 8080 port

After this application is ready to accept requests

There is a postman collection in the postman_collection folder

Data to the database was imported with the Compass tool. Docker volume for mongodb is located in the "data" folder in the root of the project. Therefore after the cloning repository it might be necessary to delete "data" folder and import data again to avoid authentication issues.
