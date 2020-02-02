node {
    def mvnHome
    stage('Preparation') {
        checkout scm
        // Get the Maven tool.
        // ** NOTE: This 'M3' Maven tool must be configured
        // **       in the global configuration.           
        mvnHome = tool 'M3'
    }
    stage('Build') {
        // Run the maven build
        ansiColor('xterm') {
	        if (isUnix()) {
	            sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
	        } else {
	            bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
	        }
        }
    }
    stage('Results') {
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
        recordIssues healthy: 1, unhealthy: 10, qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]], tools: [java(), javaDoc(), mavenConsole()]
        archiveArtifacts allowEmptyArchive: true, artifacts: 'target/*.jar', fingerprint: true
    }
}