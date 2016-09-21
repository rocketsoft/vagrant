import groovy.json.JsonSlurperClassic

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

stage('Pre Cleaning') {
    node('VAGRANTHOST') {
        catchError {
            print "Removing old vagrant test boxes is exists"
            bat 'rem vagrant destroy --force'
        }
    }
}

stage('Build') {
    node('BUILDMACHINE') {
        print 'Store system build has started'
        print 'Store system build is finished'
        
        print 'POSSimulator build has started'
        print 'POSSimulator build is finished'
    }
}

try {
    stage('Test Machine Setup') {
        node('VAGRANTHOST') {
            print "Checkout Scripts"
            git branch: 'awards_ust', credentialsId: 'rocketsoft', url: 'https://github.com/rocketsoft/vagrant.git'
            
            print "Starting vagrant test machines"
            bat 'rem vagrant up'
            //sleep time: 2, unit: 'MINUTES'
            
            print "Upgrading store system software in test machines"
            bat 'rem vagrant provision'
            
            print 'Test machines are up-to-date'
        }
    }

    stage('Unit Test') {
        node('UNITTEST') {
            print "Unit Test has started"
            print "Unit Test is finished"
        }
    }

    stage('BDD Tests') {
        node('VAGRANTHOST') {
            
            print "Serving BDD Tests to nodes for parallel execution"
            def config = jsonParse( readFile('bddtests.json'))
            
            def bddTests = [:]
            
            for( def i = 0; i < config['bddtests'].size(); i++) {
                
                def test = config['bddtests'].get(i)
                bddTests['bddtest_${i}']  = {
                    
                    def testScripts = ""
                    test['scripts'].each {
                        testScripts = testScripts + ' ' + it
                    }
                    
                    def testTags = ""
                    test['tags'].each {
                        testTags = testTags + 'AND' + it
                    }
                    
                    def testNodes = ""
                    test['node'].each {
                        testNodes = testNodes + ' && ' + it
                    }
                    
                    node( testNodes) {
                        print "BDD Test has started"
                        if ( testScripts != "") {
                            if ( testTags != "" ) {
                                print "python -m robot -i ${testTags} ${testScripts} output_${i}.xml"
                            } else {
                                print "python -m robot ${testScripts} output_${i}.xml"
                            }
                        }
                        print "BDD Test is finished"
                    }
                }
            }
            
            parallel bddTests
        }
    }

    stage('Reporting') {
        node('VAGRANTHOST') {
            print "Reporting has started"
            print "Reporting is finished"
        }
    }
} catch (err) {
    echo "Caught: ${err}"
    stage(' Post Cleaning') {
        node('VAGRANTHOST') {
            print "Cleaning (POST) has started"
            print "Cleaning (POST) is finished"
        }
    }

    error 'Build Failed'
}