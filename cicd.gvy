pipeline {
    agent any
    tools {
        jdk 'Java11'
    }
    stages {
        stage('compile') {
	         steps {
                // step1 
                echo 'compiling..'
		            git url: 'https://github.com/Kamalanath-Naidu/PetClinic'
		            sh script: '/opt/maven/bin/mvn compile'
           }
        }
        stage('codereview-pmd') {
	         steps {
                // step2
                echo 'codereview..'
		            sh script: '/opt/maven/bin/mvn -P metrics pmd:pmd'
           }
	         post {
               success {
		             recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
               }
           }		
        }
        stage('unit-test') {
	          steps {
                // step3
                echo 'unittest..'
	               sh script: '/opt/maven/bin/mvn test'
            }
	          post {
               success {
                   junit 'target/surefire-reports/*.xml'
               }
            }			
        }
        stage('Build Docker Image') {
	         steps {
                // step5
                echo 'package......'
		            sh script: '/opt/maven/bin/mvn package'	
           }		
        }
        stage('Push Docker Image') {
	         steps {
              withDockerRegistry(credentialsId: 'docker_hub_login', url: 'https://index.docker.io/v1/') {
                    sh script: 'cd  $WORKSPACE'
                    sh script: 'docker build --file Dockerfile --tag docker.io/discipleofpeace/petclinic:$BUILD_NUMBER .'
                    sh script: 'docker push docker.io/discipleofpeace/petclinic:$BUILD_NUMBER'
              }	
           }		
        }
    stage('DeployToProduction') {
  	   steps {
              sh 'ansible-playbook --inventory /tmp/inv $WORKSPACE/deploy/deploy-kube.yml --extra-vars "env=qa build=$BUILD_NUMBER"'
	   }
	   //post { 
           //   always { 
           //     cleanWs() 
	   //   }
	   //}
	}
    }
}
