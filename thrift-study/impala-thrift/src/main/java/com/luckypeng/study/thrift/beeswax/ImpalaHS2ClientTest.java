package com.luckypeng.study.thrift.beeswax;

import org.apache.hive.service.cli.thrift.*;
import org.apache.impala.thrift.ImpalaHiveServer2Service;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import java.util.List;
import java.util.Scanner;

/**
 * Impala HiveServer2 方式连接
 * @author coalchan
 * @date 2020/02/19
 */
public class ImpalaHS2ClientTest {
    private static String HOST = "kuber01";
    private static int PORT = 21050;
    private static int TIMEOUT = 60;
    private static String USER_NAME = "thrift-test";

    private static TSocket transport;
    private static TCLIService.Client client;
    private static TSessionHandle session;

    public static void main(String[] args) {
        getClient();
        getSession();

        Scanner sc = new Scanner(System.in);
//        String sql = "select * from import.paiban_table limit 201";

        String line;

        System.out.println(">>>>>>>>Input Statement Line or enter `quit` to exit<<<<<<<<<");
        while((line = sc.nextLine()) != null) {
            if(line.trim().equalsIgnoreCase("quit")) {
                System.out.println("Bye!");
                break;
            }
            try {
                testThriftClient(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(">>>>>>>>Input Statement Line or enter `quit` to exit<<<<<<<<<");
        }

        closeSession();
        close();
    }

    private static void getClient() {
        transport = new TSocket(HOST, PORT, TIMEOUT);
        try {
            transport.open();
        } catch (TTransportException e) {
            e.printStackTrace();
        }
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        client = new ImpalaHiveServer2Service.Client(protocol);
    }

    private static void getSession() {
        TOpenSessionReq openReq = new TOpenSessionReq();
        openReq.setClient_protocol(TProtocolVersion.HIVE_CLI_SERVICE_PROTOCOL_V1);
        openReq.setUsername(USER_NAME);
        TOpenSessionResp openResp = null;
        try {
            openResp = client.OpenSession(openReq);
        } catch (TException e) {
            e.printStackTrace();
        }
        System.out.println("server protocol version is: " + openResp.getServerProtocolVersion());
        session = openResp.getSessionHandle();
    }

    private static TExecuteStatementResp submitQuery(String sql) throws TException {
        TExecuteStatementReq execReq = new TExecuteStatementReq(session, sql);
        //设置异步
        execReq.setRunAsync(true);
        TExecuteStatementResp execResp = client.ExecuteStatement(execReq);
        return execResp;
    }

    public static void testThriftClient(String sql) throws Exception {
        TExecuteStatementResp execResp = submitQuery(sql);

        TStatus status = execResp.getStatus();
        if (status.getStatusCode() == TStatusCode.ERROR_STATUS ||
                status.getStatusCode() == TStatusCode.INVALID_HANDLE_STATUS) {
            String msg = status.getErrorMessage();
            System.out.println("query error: " + msg);
            return;
        }

        TOperationHandle tOperationHandle = execResp.getOperationHandle();

        TOperationState queryHandleStatus;
        while (true) {
            queryHandleStatus = getQueryHandleStatus(tOperationHandle);
            if (queryHandleStatus == TOperationState.PENDING_STATE) {
                System.out.println("pending...");
            } else if (queryHandleStatus == TOperationState.RUNNING_STATE) {
                getQueryLog(tOperationHandle);
            } else if (queryHandleStatus == TOperationState.FINISHED_STATE) {
                break;
            } else if (queryHandleStatus == TOperationState.ERROR_STATE) {
                System.out.println("Query caused exception !");
                break;
            }
            Thread.sleep(100);
        }

        boolean done = false;
        while (queryHandleStatus == TOperationState.FINISHED_STATE && !done) {
            TFetchResultsResp resultsResp = getResult(tOperationHandle);
            if (!resultsResp.hasMoreRows) {
                done = true;
            }
        }

        closeOperation(tOperationHandle);
    }

    private static void closeOperation(TOperationHandle tOperationHandle) throws TException {
        TCloseOperationReq closeReq = new TCloseOperationReq(tOperationHandle);
        client.CloseOperation(closeReq);
    }

    private static void closeSession() {
        TCloseSessionReq closeConnectionReq = new TCloseSessionReq(session);
        try {
            client.CloseSession(closeConnectionReq);
        } catch (TException e) {
            e.printStackTrace();
        }
    }

    private static void close() {
        if (transport != null) {
            transport.close();
        }
    }

    public static TFetchResultsResp getResult(TOperationHandle tOperationHandle) throws TException {
        //获取列名
        TGetResultSetMetadataReq metadataReq = new TGetResultSetMetadataReq(tOperationHandle);
        TGetResultSetMetadataResp metadataResp = client.GetResultSetMetadata(metadataReq);
        List<TColumnDesc> columns = metadataResp.getSchema().getColumns();
        columns.forEach(column -> System.out.print(column.getColumnName() + "\t"));
        System.out.println();

        //获取数据
        TFetchResultsReq fetchReq = new TFetchResultsReq(tOperationHandle, TFetchOrientation.FETCH_NEXT, 100);

        TFetchResultsResp resultsResp = client.FetchResults(fetchReq);
        TStatus status = resultsResp.getStatus();
        if (status.getStatusCode() == TStatusCode.ERROR_STATUS) {
            String msg = status.getErrorMessage();
            System.out.println(msg + "," + status.getSqlState() + "," + status.getErrorCode() + "," + status.isSetInfoMessages());
        }
        TRowSet resultsSet = resultsResp.getResults();
        List<TRow> resultRows = resultsSet.getRows();

        for (TRow resultRow : resultRows) {
            List<TColumnValue> rows = resultRow.getColVals();
            rows.forEach(row -> System.out.print(getValue(row) + "\t"));
            System.out.println();
        }

        return resultsResp;
    }

    private static Object getValue(TColumnValue field) {
        if (field.isSetStringVal()) {
            return field.getStringVal().getValue();
        } else if (field.isSetDoubleVal()) {
            return field.getDoubleVal().getValue();
        } else if (field.isSetI16Val()) {
            return field.getI16Val().getValue();
        } else if (field.isSetI32Val()) {
            return field.getI32Val().getValue();
        } else if (field.isSetI64Val()) {
            return field.getI64Val().getValue();
        } else if (field.isSetBoolVal()) {
            return field.getBoolVal().isValue();
        } else if (field.isSetByteVal()) {
            return field.getByteVal().getValue();
        } else {
            return field.getFieldValue();
        }
    }

    public static void getQueryLog(TOperationHandle tOperationHandle) throws Exception {
        TGetLogReq tGetLogReq = new TGetLogReq(tOperationHandle);
        TGetLogResp logResp = client.GetLog(tGetLogReq);
        String log = logResp.getLog();
        if (log != null) {
            System.out.println("GetLog: " + log);
        }
    }

    public static TOperationState getQueryHandleStatus(TOperationHandle tOperationHandle) throws Exception {
        TOperationState tOperationState = null;
        if (tOperationHandle != null) {
            TGetOperationStatusReq statusReq = new TGetOperationStatusReq(tOperationHandle);
            TGetOperationStatusResp statusResp = client.GetOperationStatus(statusReq);
            tOperationState = statusResp.getOperationState();
        }
        return tOperationState;
    }
}
