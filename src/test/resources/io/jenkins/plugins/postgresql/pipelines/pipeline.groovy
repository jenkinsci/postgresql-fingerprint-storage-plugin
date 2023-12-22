pipeline {
    agent {
        label('test-agent')
    }
    stages {
        stage('Write file') {
            steps {
                sh(script: 'echo "bar" > foo.txt')
            }
        }
        stage('Archive file') {
            steps {
                archiveArtifacts(artifacts: '**/*.txt', fingerprint: true, allowEmptyArchive: false)
            }
        }
    }
}
