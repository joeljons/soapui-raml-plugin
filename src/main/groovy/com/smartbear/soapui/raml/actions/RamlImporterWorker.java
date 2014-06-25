package com.smartbear.soapui.raml.actions;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.rest.RestRequest;
import com.eviware.soapui.impl.rest.RestResource;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.mock.RestMockService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.registry.RestRequestStepFactory;
import com.eviware.soapui.support.UISupport;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.smartbear.soapui.raml.RamlImporter;

import java.io.File;

/**
* Created by ole on 21/06/14.
*/
public class RamlImporterWorker extends Worker.WorkerAdapter {
    private final String finalExpUrl;
    private WsdlProject project;
    private XFormDialog dialog;

    public RamlImporterWorker(String finalExpUrl, WsdlProject project, XFormDialog dialog) {
        this.finalExpUrl = finalExpUrl;
        this.project = project;
        this.dialog = dialog;
    }

    public Object construct(XProgressMonitor monitor) {

        try {
            // create the importer and import!
            RamlImporter importer = new RamlImporter(project);
            importer.setCreateSampleRequests( dialog.getBooleanValue(CreateRamlProjectAction.Form.CREATE_REQUESTS));
            SoapUI.log("Importing RAML from [" + finalExpUrl + "]");
            SoapUI.log( "CWD:" + new File(".").getCanonicalPath());
            RestMockService mockService = null;

            if( dialog.getBooleanValue( CreateRamlProjectAction.Form.GENERATE_MOCK ))
            {
                mockService = project.addNewRestMockService( "Generated MockService" );
                importer.setRestMockService( mockService );
            }

            RestService restService = importer.importRaml(finalExpUrl);

            if( mockService != null )
                mockService.setName( restService.getName() + " MockService" );

            if( dialog.getBooleanValue( CreateRamlProjectAction.Form.GENERATE_TESTSUITE))
            {
                WsdlTestSuite testSuite = project.addNewTestSuite( "TestSuite" );
                generateTestSuite( restService, testSuite );
            }

            UISupport.select(restService);
            Analytics.trackAction("ImportRAML");

            return restService;
        } catch (Throwable e) {
            UISupport.showErrorMessage(e);
        }

        return null;
    }

    public void generateTestSuite(RestService service, WsdlTestSuite testSuite) {
        for (RestResource resource : service.getAllResources()) {

            WsdlTestCase testCase = testSuite.addNewTestCase(resource.getName() + " TestCase");
            testCase.setDescription("TestCase generated for REST Resource [" + resource.getName() + "] located at ["
                    + resource.getFullPath(false) + "]");

            if (resource.getRequestCount() > 0) {
                for (int x = 0; x < resource.getRequestCount(); x++) {
                    RestRequest request = resource.getRequestAt(x);
                    testCase.addTestStep(RestRequestStepFactory.createConfig(request, request.getName()));
                }
            }
        }
    }

}
