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
    node('BUILD') {
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
				if( test["enabled"] == null || 
					test["enabled"] == true) {
					    
					bddTests["bddtest_${i}"]  = {
						
						def testScripts = ""
						for( def j = 0; j < test['scripts'].size(); j++) {
							if( j==0) {
								testScripts = test['scripts'].get(j)
							} else {
								testScripts = testScripts + ' ' + test['scripts'].get(j)
							}
						}
						
						def testTags = ""
						for( def j = 0; j < test['tags'].size(); j++) {
							if( j==0) {
								testTags = test['tags'].get(j)
							} else {
								testTags = testTags + 'AND' +  test['tags'].get(j)
							}
						}
						
						def testNodes = ""
						for( def j = 0; j < test['node'].size(); j++) {
							if( j==0) {
								testNodes = test['node'].get(j)
							} else {
								testNodes = testNodes + ' && ' + test['node'].get(j)
							}
						}

						node( testNodes) {
							print "BDD Test has started"
							if ( testScripts != "") {
								
								env.ROBOTSCRIPT="${testScripts}"
								env.ROBOTTAGS="${testTags}"
								env.ROBOTOUTPUT="output_${i}.xml"
								
								bat '''set PYTHONPATH=C:\\strsystm\\AcceptanceTest\\tools\\python27
									set SWIGWINPATH=C:\\strsystm\\AcceptanceTest\\tools\\swigwin-3.0.8
									set PYTHONSCRIPTSPATH=C:\\strsystm\\AcceptanceTest\\tools\\python27\\scripts
									set ACCEPTANCETESTBIN=C:\\strsystm\\AcceptanceTest\\bin
									set PATH=%PYTHONPATH%;%PYTHONSCRIPTSPATH%;%ACCEPTANCETESTBIN%;%SWIGWINPATH%;%PATH%
									set PATH=C:\\strsystm\\bin;C:\\strsystm\\dll;%PATH%
									
									pushd C:\\strsystm\\AcceptanceTest\\src
									if defined ROBOTTAGS (
										python -m robot -i %ROBOTSCRIPT% %ROBOTTAGS% %ROBOTOUTPUT%
									) else (
										python -m robot %ROBOTTAGS% %ROBOTOUTPUT% 
									)
									popd
								'''

							}
							print "BDD Test is finished"
						}
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