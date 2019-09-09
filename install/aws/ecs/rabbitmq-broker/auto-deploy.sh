#!/bin/bash
#  
# Copyright 2019 i8c N.V. (www.i8c.be)
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
region=`aws configure get region`
clustername="sag-c8y-nb-iot-demo"
reponame="sag-c8y-nb-iot-demo-rabbitmq-broker"

RABBITMQ_DEFAULT_USER="user1"
RABBITMQ_DEFAULT_PASS="pass1"
RABBITMQ_DEFAULT_VHOST="vhost1"



$(aws ecr get-login --no-include-email --region $region)
repositoryUri=""
registryId=""
while [ "$repositoryUri" == "" ];
do
	aws ecr describe-repositories --repository-names $reponame > tmp
	
	#Loop over all lines in tmp:
	while read LINE; 
	do
		LINE=${LINE:1}
		if [ "$LINE" != "" ] #get rid of lines that were originally '[',  ']', '{', ...
		then
			KEY=${LINE:0:`expr index "$LINE" '"'`-1}
			if [ "$KEY" == "repositoryUri" ] # filters out lines that contain "repositoryUri"
			then
				# get link from $LINE:
				LINK=${LINE:`expr length "$KEY"`+4}
				LINK=${LINK:0:`expr index "$LINK" '"'`-1}
				
				echo $LINK
				# echo ${LINK:`expr length "$LINK"`-`expr length "$reponame"`}
				
				# ugly but working way of checking if $LINK contains $reponame:
				if [ "${LINK:`expr length "$LINK"`-`expr length "$reponame"`}" == "$reponame" ]
				then
					echo "repo found"
					repositoryUri=$LINK
					registryId=${LINK:0:12}
					break
				fi
			fi
		fi
	done < tmp
	
	#if while loop didn't find the repository, create repo, cluster & log group
	if [ "$repositoryUri" == "" ]
	then 
		echo "repo not found, creating"
		aws ecr create-repository --repository-name "$reponame"
		echo "creating cluster"
		aws ecs create-cluster --cluster-name $clustername
		echo "creating log group"
		aws logs create-log-group --log-group-name "ecs/""$clustername"-log
	fi
done
rm tmp
echo "---"
echo "repositoryUri: "$repositoryUri
echo "region: "$region
echo "registryId: "$registryId
echo "building"
sleep 1
pushd ../../../docker/dockerfiles/rabbitmq-broker/
./build.sh
docker tag "$reponame":latest "$repositoryUri"
echo "pushing"
docker push "$repositoryUri"
popd -1


#check if service exists:
serviceexists="false"
aws ecs list-services --cluster $clustername > tmp
while read LINE; 
do
	LINE=${LINE:1}
	if [ "$LINE" != "" ] #get rid of lines that were originally '[',  ']', '{', ...
	then
		LINE=${LINE::`expr index "$LINE" '"'`-1}
		echo $LINE
		LINE=${LINE:`expr length "$LINE"`-`expr length $reponame`:`expr length $reponame`}
		if [ "$LINE" == "$reponame" ]
		then
			serviceexists="true"
		fi
	fi
done < tmp
rm tmp


if [ $serviceexists == "false" ]
then 
	echo "creating task definition"
	aws ecs register-task-definition --cli-input-json "{\"family\": \""$reponame"-task\",\"executionRoleArn\": \"arn:aws:iam::"$registryId":role/ecsTaskExecutionRole\",\"containerDefinitions\":[{\"name\":\""$reponame"-task-definition\",\"image\":\""$repositoryUri":latest\",\"logConfiguration\":{\"logDriver\":\"awslogs\",\"options\":{\"awslogs-group\":\"ecs/"$clustername"-log\",\"awslogs-region\":\""$region"\",\"awslogs-stream-prefix\":\"ecs\"}},\"cpu\":10,\"memory\":300,\"essential\":true,\"environment\":[{\"name\":\"RABBITMQ_DEFAULT_PASS\",\"value\":\""$RABBITMQ_DEFAULT_PASS"\"},{\"name\":\"RABBITMQ_DEFAULT_USER\",\"value\":\""$RABBITMQ_DEFAULT_USER"\"},{\"name\":\"RABBITMQ_DEFAULT_VHOST\",\"value\":\""$RABBITMQ_DEFAULT_VHOST"\"}]}],\"requiresCompatibilities\":[\"FARGATE\"],\"networkMode\":\"awsvpc\",\"cpu\":\"256\",\"memory\":\"512\"}"
	
	# 1. create/get vpc
	# 2. create subnet using vpc
	# 3. create security group using same vpc
	
	# 1. get first vpc or create one if none exist:
	vpcId=""
	cidrblock=""
	while [ "$vpcId" == "" ];
	do
		aws ec2 describe-vpcs --filters "Name=isDefault,Values=true">tmp
		while read LINE; 
		do
			LINE=${LINE:1}
			if [ "$LINE" != "" ] #get rid of lines that were originally '[',  ']', '{', ...
			then
				KEY=${LINE:0:`expr index "$LINE" '"'`-1}
				
				
				if [ "$KEY" == "VpcId" ] # filters out lines that contain key "VpcId"
				then
					# get link from $LINE:
					LINK=${LINE:`expr length "$KEY"`+4}
					LINK=${LINK:0:`expr index "$LINK" '"'`-1}
					
					vpcId=$LINK
				fi
				
				if [ "$KEY" == "CidrBlock" ] # filters out lines that contain key "CidrBlock"
				then
					# get link from $LINE:
					LINK=${LINE:`expr length "$KEY"`+4}
					LINK=${LINK:0:`expr index "$LINK" '"'`-1}
					
					cidrblock=$LINK
				fi
				
				if [ "$vpcId" != "" ] && [ "$cidrblock" != "" ]
				then
					break
				fi
				
			fi
		done <tmp
		rm tmp
	
		# if no vpc found, create one:
		if [ "$vpcId" == "" ]
		then
			echo no vpc found, creating one
			aws ec2 create-vpc --cidr-block 10.0.0.0/16
		fi
	done
	echo vpc: $vpcId
	echo cidrblock: $cidrblock
	
	
	# 2. ensure that the security group exists for this vpc: (can just be created and error when it already exists can be ignored)
	aws ec2 create-security-group --group-name $reponame --description "ECS allowed ports" --vpc-id $vpcId
	
	aws ec2 describe-security-groups --group-names $reponame>tmp

	securityGroupId=""
	while read LINE; 
	do
		LINE=${LINE:1}
		if [ "$LINE" != "" ] #get rid of lines that were originally '[',  ']', '{', ...
		then
			KEY=${LINE:0:`expr index "$LINE" '"'`-1}
			if [ "$KEY" == "GroupId" ] # filters out lines that contain "GroupId"
			then
				# get link from $LINE:
				LINK=${LINE:`expr length "$KEY"`+4}
				LINK=${LINK:0:`expr index "$LINK" '"'`-1}
				
				securityGroupId=$LINK
			fi
		fi
	done <tmp
	rm tmp
	echo securityGroupId: $securityGroupId
	# open port 5672 tcp in this security group
	aws ec2 authorize-security-group-ingress --group-id $securityGroupId --protocol tcp --port 5672 --cidr 0.0.0.0/0
	
	# 3. get first subnet or create one:	
	subnetId=""
	while [ "$subnetId" == "" ];
	do
		aws ec2 describe-subnets --filters "Name=vpc-id,Values="$vpcId>tmp
		while read LINE; 
		do
			LINE=${LINE:1}
			if [ "$LINE" != "" ] #get rid of lines that were originally '[',  ']', '{', ...
			then
				KEY=${LINE:0:`expr index "$LINE" '"'`-1}
				if [ "$KEY" == "SubnetId" ] # filters out lines that contain "SubnetId"
				then
					# get link from $LINE:
					LINK=${LINE:`expr length "$KEY"`+4}
					LINK=${LINK:0:`expr index "$LINK" '"'`-1}
					
					subnetId=$LINK
					break
				fi
			fi
		done <tmp
		rm tmp
	
		# if no subnet found, create one:
		if [ "$subnetId" == "" ]
		then
			echo no subnet found, creating one
			aws ec2 create-subnet --vpc-id $vpcId --cidr-block $cidrblock
		fi
	done
	echo subnetId: $subnetId
	
	
	echo "creating cluster service"
	aws ecs create-service --cluster "$clustername" --service-name "$reponame" --task-definition "$reponame"-task --desired-count 1 --launch-type "FARGATE" --network-configuration "awsvpcConfiguration={subnets=["$subnetId"],securityGroups=["$securityGroupId"],assignPublicIp=ENABLED}"
else
	echo "starting new image"
	aws ecs update-service --cluster "$clustername" --service "$reponame" --force-new-deployment
fi

sleep 3
echo "opening browserpage: https://"$region".console.aws.amazon.com/ecs/home?region="$region"#/clusters/"$clustername"/tasks"
python -mwebbrowser "https://"$region".console.aws.amazon.com/ecs/home?region="$region"#/clusters/"$clustername"/tasks"
echo "== rabbitmq-broker made =="

sleep 3