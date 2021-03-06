/*
 * NorthRidge Software, LLC - Copyright (c) 2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nridge.examples.oss.ds_solr;

import com.google.gson.stream.JsonWriter;
import com.nridge.core.app.mgr.AppMgr;
import com.nridge.core.app.mgr.Task;
import com.nridge.core.base.doc.Document;
import com.nridge.core.base.ds.DSCriteria;
import com.nridge.core.base.ds.DSException;
import com.nridge.core.base.field.Field;
import com.nridge.core.base.field.data.DataBag;
import com.nridge.core.base.field.data.DataTable;
import com.nridge.core.base.io.console.DataTableConsole;
import com.nridge.core.base.io.console.DocumentConsole;
import com.nridge.core.base.io.xml.DataBagXML;
import com.nridge.core.base.io.xml.DocumentXML;
import com.nridge.core.base.std.NSException;
import com.nridge.core.base.std.Sleep;
import com.nridge.core.io.csv.DataTableCSV;
import com.nridge.core.io.gson.DocumentJSON;
import com.nridge.core.io.log.DSCriteriaLogger;
import com.nridge.core.io.log.DataBagLogger;
import com.nridge.core.io.log.DataTableLogger;
import com.nridge.ds.solr.Solr;
import com.nridge.ds.solr.SolrDS;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The DSSolrTask implements a collection of methods that the
 * Application Manager will invoke over the lifecycle of a Java
 * thread.
 */
@SuppressWarnings("FieldCanBeLocal")
class DSSolrTask implements Task
{
    private final String mRunName = "solr";
    private final String mTestName = "solr";

    private final String CFG_PROPERTY_PREFIX = "app.solr";

    private final String PART_FILE_NAME = "part_documents.csv";
    private final String SCHEMA_FILE_NAME = "ds_solr_schema.xml";


    private AppMgr mAppMgr;
    private AtomicBoolean mIsAlive;

	/**
     * Returns the name of the run task.  This name will be used
     * by the application manager to identify which task in the
     * list to run (based on command line arguments).
     *
     * @return Name of the run task.
     */
    @Override
    public String getRunName()
    {
        return mRunName;
    }

	/**
     * Returns the name of the test task.  This name will be used
     * by the application manager to identify which task in the
     * list to test (based on command line arguments).
     *
     * @return Name of the test task.
     */
    @Override
    public String getTestName()
    {
        return mTestName;
    }

	/**
     * Returns <i>true</i> if this task was properly initialized
     * and is currently executing.
     *
     * @return <i>true</i> or <i>false</i>
     */
    @Override
    public boolean isAlive()
    {
        if (mIsAlive == null)
            mIsAlive = new AtomicBoolean(false);

        return mIsAlive.get();
    }

	/**
     * If this task is scheduled to be executed (e.g. its run/test
     * name matches the command line arguments), then this method
     * is guaranteed to be executed prior to the thread being
     * started.
     *
     * @param anAppMgr Application manager instance.
     *
     * @throws NSException Application specific exception.
     */
    @Override
    public void init(AppMgr anAppMgr)
        throws NSException
    {
        mAppMgr = anAppMgr;
        Logger appLogger = mAppMgr.getLogger(this, "init");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

		mIsAlive = new AtomicBoolean(false);

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);

        mIsAlive.set(true);
    }

	/**
     * Each task supports a method dedicated to testing or exercising
     * a subset of application features without having to run the
     * mainline thread of task logic.
     *
     * @throws NSException Application specific exception.
     */
    @Override
    public void test()
        throws NSException
    {
        Logger appLogger = mAppMgr.getLogger(this, "test");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

        if (! isAlive())
        {
            appLogger.error("Initialization failed - must abort test method.");
            return;
        }

        appLogger.info("The test method was invoked.");
        Sleep.forSeconds(1);

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);
    }

    private void toXML(Document aDocument)
    {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter))
        {
            DocumentXML documentXML = new DocumentXML(aDocument);
            documentXML.save(printWriter);
            System.out.printf("%nDocument XML Output%n%s%n", stringWriter.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void toJSON(Document aDocument)
    {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = new JsonWriter(stringWriter))
        {
            jsonWriter.setIndent(" ");
            DocumentJSON documentJSON = new DocumentJSON(aDocument);
            documentJSON.save(jsonWriter);
            System.out.printf("%nDocument JSON Output%n%s%n", stringWriter.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void toConsole(DataTable aTable, String aTitle)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        DataTableConsole dataTableConsole = new DataTableConsole(aTable);
        dataTableConsole.write(printWriter, aTitle);
        printWriter.close();
        System.out.printf("%n%s%n", stringWriter.toString());
    }

    private SolrDS createSolrDataSource()
    {
        SolrDS solrDS;
        Logger appLogger = mAppMgr.getLogger(this, "createSolrDataSource");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

        String schemaPathFileName = String.format("%s%c%s", mAppMgr.getString(mAppMgr.APP_PROPERTY_DS_PATH),
            File.separatorChar, SCHEMA_FILE_NAME);
        DataBagXML dataBagXML = new DataBagXML();
        try
        {
            dataBagXML.load(schemaPathFileName);
            DataBag schemaBag = dataBagXML.getBag();
            solrDS = new SolrDS(mAppMgr);
            solrDS.setSchema(schemaBag);
            solrDS.setCfgPropertyPrefix(CFG_PROPERTY_PREFIX);
        }
        catch (Exception e)
        {
            solrDS = null;
            e.printStackTrace();
        }

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);

        return solrDS;
    }

    private ArrayList<Document> tableToDocumentArray(DataTable aTable)
    {
        String docType;
        DataBag dataBag;

        int rowCount = aTable.rowCount();
        ArrayList<Document> documentList = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++)
        {
            dataBag = aTable.getRowAsBag(row);
            docType = dataBag.getValueAsString("nsd_doc_type");
            documentList.add(new Document(docType, dataBag));
        }

        return documentList;
    }

    private void addDocuments(SolrDS aSolrDS)
    {
        Logger appLogger = mAppMgr.getLogger(this, "addDocuments");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

        String partPathFileName = String.format("%s%cdata%c%s", mAppMgr.getString(mAppMgr.APP_PROPERTY_INS_PATH),
                                                File.separatorChar, File.separatorChar, PART_FILE_NAME);
        DataTable dataTable = new DataTable(aSolrDS.getSchema());
        DataTableCSV dataTableCSV = new DataTableCSV(dataTable);
        try
        {
            dataTableCSV.load(partPathFileName, true);
            ArrayList<Document> documentList = tableToDocumentArray(dataTable);
            aSolrDS.add(documentList);
            aSolrDS.commit();
        }
        catch (DSException | IOException e)
        {
            String msgStr = String.format("%s: %s", partPathFileName, e.getMessage());
            appLogger.error(msgStr);
            e.printStackTrace();
        }

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);
    }

    private void queryDocuments(SolrDS aSolrDS)
    {
        Logger appLogger = mAppMgr.getLogger(this, "queryDocuments");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

        DSCriteria dsCriteria = new DSCriteria("Solr Query General");
        dsCriteria.add(Solr.FIELD_HANDLER_NAME, Field.Operator.EQUAL, "/enterprise");
        dsCriteria.add(Solr.FIELD_QUERY_NAME, Field.Operator.EQUAL, Solr.QUERY_ALL_DOCUMENTS);
        dsCriteria.add("nsd_is_latest", Field.Operator.EQUAL, "true");

        DSCriteriaLogger dsCriteriaLogger = new DSCriteriaLogger(appLogger);
        dsCriteriaLogger.writeFull(dsCriteria);

        try
        {
            Document solrDocument = aSolrDS.fetch(dsCriteria);
            if (Solr.isResponsePopulated(solrDocument))
            {
                toXML(solrDocument);
                toJSON(solrDocument);
                DataTableLogger dataTableLogger = new DataTableLogger(appLogger);
                dataTableLogger.writeSimple(Solr.getResponse(solrDocument));
                if (Solr.isFacetFieldsPopulated(solrDocument))
                    dataTableLogger.writeSimple(Solr.getFacetFields(solrDocument));
                if (Solr.isFacetRangePopulated(solrDocument))
                    dataTableLogger.writeSimple(Solr.getFacetRange(solrDocument));
                if (Solr.isFacetQueriesPopulated(solrDocument))
                    dataTableLogger.writeSimple(Solr.getFacetQueries(solrDocument));
                if (Solr.isFacetPivotPopulated(solrDocument))
                    dataTableLogger.writeSimple(Solr.getFacetPivot(solrDocument));
                if (Solr.isStatisticsPopulated(solrDocument))
                    dataTableLogger.writeSimple(Solr.getStatistics(solrDocument));
                if (Solr.isSpellingPopulated(solrDocument))
                {
                    DataBagLogger dataBagLogger = new DataBagLogger(appLogger);
                    dataBagLogger.writeSimple(Solr.getSpelling(solrDocument));
                }
            }
        }
        catch (DSException e)
        {
            String msgStr = String.format("%s: %s", dsCriteria.getName(), e.getMessage());
            appLogger.error(msgStr);
            e.printStackTrace();
        }

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);
    }

    /**
     * Exercise some of the core Apache Solr features in the Expiscor
     * class library.
     */
    private void exerciseSolr()
    {
        SolrDS solrDS = createSolrDataSource();
        if (solrDS != null)
        {
            addDocuments(solrDS);
            queryDocuments(solrDS);
        }
    }

	/**
     * The {@link Runnable}.run() will be executed after the task
     * has been successfully initialized.  This method is where
     * the application specific logic should be concentrated for
     * the task.
     */
    @Override
    public void run()
    {
        Logger appLogger = mAppMgr.getLogger(this, "run");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

        if (! isAlive())
        {
            appLogger.error("Initialization failed - must abort run method.");
            return;
        }

        appLogger.info("Exercise Apache Solr Features");
        exerciseSolr();

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);
    }

	/**
     * Once the task has completed its run, then this method
     * will be invoked by the Application Manager to enable
     * the task to release any resources it was holding.
     * <p>
     * <b>Note:</b>If the JVM detects and external shutdown
     * event (e.g. service is being stopped), then the
     * Application Manager will asynchronously invoke this
     * in hopes that the task can save its state prior to
     * the process exiting.
	 * </p>
     */
    @Override
    public void shutdown()
    {
        Logger appLogger = mAppMgr.getLogger(this, "shutdown");

        appLogger.trace(mAppMgr.LOGMSG_TRACE_ENTER);

        if (isAlive())
        {
            appLogger.info("The shutdown method was invoked.");
            Sleep.forSeconds(1);
            mIsAlive.set(false);
        }

        appLogger.trace(mAppMgr.LOGMSG_TRACE_DEPART);
    }
}
