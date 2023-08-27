pipeline {
    agent {
        kubernetes {
            inheritFrom 'kaniko'
            defaultContainer 'kaniko'
            yaml """
            apiVersion: v1
            kind: Pod
            metadata:
            spec:
              containers:
              - name: kaniko
                image: 'gcr.io/kaniko-project/executor:debug'
                command:
                - sleep
                args:
                - infinity
              - name: maven
                image: maven:3.9.3-amazoncorretto-20
                command:
                - sleep
                args:
                - infinity
                tty: true
              restartPolicy: Never
            """
        }
    }

    environment {
        gitUrl = "https://github.com/danielbeltejar/pokemon-weather-map/"
        project = "pro-pokemon-weather-map"
        projectRoot = "${gitUrl.tokenize("/")[-1].replaceAll(".git", "")}"
        projectFolder = "webservice-date"
        imageName = "${gitUrl.tokenize("/")[-1].replaceAll(".git", "")}-${projectFolder}"
        kind = "deployment"
        kindName = "date-forecast-ws-deployment"
        
        containerName = ""
        imageTag = ""
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                container('kaniko') {
                    checkout([$class: 'GitSCM',
                              branches: [[name: 'main']],
                              userRemoteConfigs: [[url: "${gitUrl}"]]
                    ])
                }
            }
        }
        stage('Build artifact') {
            steps {
                container('maven') {
                    sh """
                    cd ${projectFolder}
                    mvn clean package -DfinalName=${projectFolder}
                    """
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                container('kaniko') {
                    // Build the Docker image using Kaniko
                    script {
                        def currentDate = sh(script: 'date "+%Y%m%d"', returnStdout: true).trim()
                        imageTag = "${currentDate}-${env.BUILD_NUMBER}"

                        sh """
                        cd ${projectFolder}
                        /kaniko/executor \
                        --context=`pwd` \
                        --dockerfile=`pwd`/Dockerfile \
                        --destination=registry-service.lab-registry.svc.cluster.local:5000/${imageName}:${imageTag} \
                        --destination=registry-service.lab-registry.svc.cluster.local:5000/${imageName}:latest
                        """
                    }
                }
            }
        }
        stage('Update Deployment') {
            steps {
                node('') {
                    script {
                        if (kind == "deployment") {
                            containerName = sh(script: "kubectl -n ${project} get deployment ${kindName} -o jsonpath='{.spec.template.spec.containers[0].name}'", returnStdout: true).trim()
                        } else if (kind == "cronjob") {
                            containerName = sh(script: "kubectl -n ${project} get cronjob ${kindName} -o jsonpath='{.spec.jobTemplate.spec.template.spec.containers[0].name}'", returnStdout: true).trim()
                        } else {
                            error("Unsupported kind: ${kind}")
                        }
                    }
                    script {
                        sh """
                        kubectl -n ${project} set image ${kind}/${kindName} ${containerName}=registry.server.local:5000/${imageName}:${imageTag}
                        """
                    }
                }
            }
        }
    }
}
