import groovy.json.JsonSlurperClassic

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

stage "Stage : Cleaning (PRE)"
node('WINHOST') {
    catchError {
        print "Removing old vagrant test boxes is exists"
        bat 'rem vagrant destroy --force'
    }
}

stage "Stage : Build"
node('WINHOST') {
    print 'Store system build has started'
    print 'Store system build is finished'
    
    print 'POSSimulator build has started'
    print 'POSSimulator build is finished'
}

try {
    stage "Stage : Test Machine Setup"
    node('WINHOST') {
        print "Checkout Scripts"
        git branch: 'awards', credentialsId: 'rocketsoft', url: 'https://github.com/rocketsoft/vagrant.git'
        
        print "Starting vagrant test machines"
        bat 'vagrant up'
        sleep time: 2, unit: 'MINUTES'
        
        print "Upgrading store system software in test machines"
        bat 'vagrant provision'
        
        print 'Test machines are up-to-date'
    }
    
    stage "Stage : Unit Test"
    node('WINHOST') {
        print "Unit Test has started"
        print "Unit Test is finished"
    }
    
    stage "Stage : BDD Tests"
    node('WINHOST') {
        
        print "Serving BDD Tests to nodes for parallel execution"
        def config = jsonParse( readFile('servejobs.json'))
        
        def bddTests = [:]
        
        for( def i = 0; i < config['tests'].size(); i++) {
            
            def test = config['tests'].get(i)
            bddTests["${test['node']}_${i}"]  = {
                
                node( test['node']) {
                        print "BDD Test has started"
                        print "BDD Test is finished"
                }
            }
        }
        
        parallel bddTests
    }
    
    stage "Stage : Reporting"
    node('WINHOST') {
        print "Reporting has started"
        print "Reporting is finished"
    }
} catch (err) {
    echo "Caught: ${err}"
    
    stage "Stage : Cleaning (FAILURE)"
    node('WINHOST') {
        print "Cleaning (POST) has started"
        print "Cleaning (POST) is finished"
    }
}