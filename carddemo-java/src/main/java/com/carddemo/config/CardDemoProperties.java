package com.carddemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carddemo")
public class CardDemoProperties {

    private final Features features = new Features();
    private final Jms jms = new Jms();
    private final PendingAuth pendingAuth = new PendingAuth();
    private final TransactionType transactionType = new TransactionType();

    public Features getFeatures() {
        return features;
    }

    public Jms getJms() {
        return jms;
    }

    public PendingAuth getPendingAuth() {
        return pendingAuth;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public static class Features {
        private boolean jmsEnabled;

        public boolean isJmsEnabled() {
            return jmsEnabled;
        }

        public void setJmsEnabled(boolean jmsEnabled) {
            this.jmsEnabled = jmsEnabled;
        }
    }

    public static class Jms {
        private String accountRequestQueue = "CARD.DEMO.REQUEST.ACCT";
        private String dateRequestQueue = "CARD.DEMO.REQUEST.DATE";
        private String authBridgeRequestQueue = "CARD.DEMO.AUTH.REQUEST";

        public String getAccountRequestQueue() {
            return accountRequestQueue;
        }

        public void setAccountRequestQueue(String accountRequestQueue) {
            this.accountRequestQueue = accountRequestQueue;
        }

        public String getDateRequestQueue() {
            return dateRequestQueue;
        }

        public void setDateRequestQueue(String dateRequestQueue) {
            this.dateRequestQueue = dateRequestQueue;
        }

        public String getAuthBridgeRequestQueue() {
            return authBridgeRequestQueue;
        }

        public void setAuthBridgeRequestQueue(String authBridgeRequestQueue) {
            this.authBridgeRequestQueue = authBridgeRequestQueue;
        }
    }

    public static class PendingAuth {
        private int purgeExpiryDays = 5;
        private String exportDir = System.getProperty("java.io.tmpdir") + "/carddemo-export";
        private String importSummaryFile = System.getProperty("java.io.tmpdir") + "/carddemo-import/summary.dat";
        private String importDetailFile = System.getProperty("java.io.tmpdir") + "/carddemo-import/detail.dat";

        public int getPurgeExpiryDays() {
            return purgeExpiryDays;
        }

        public void setPurgeExpiryDays(int purgeExpiryDays) {
            this.purgeExpiryDays = purgeExpiryDays;
        }

        public String getExportDir() {
            return exportDir;
        }

        public void setExportDir(String exportDir) {
            this.exportDir = exportDir;
        }

        public String getImportSummaryFile() {
            return importSummaryFile;
        }

        public void setImportSummaryFile(String importSummaryFile) {
            this.importSummaryFile = importSummaryFile;
        }

        public String getImportDetailFile() {
            return importDetailFile;
        }

        public void setImportDetailFile(String importDetailFile) {
            this.importDetailFile = importDetailFile;
        }
    }

    public static class TransactionType {
        private String batchInputFile = System.getProperty("java.io.tmpdir") + "/carddemo-trntype/trntype-update.dat";

        public String getBatchInputFile() {
            return batchInputFile;
        }

        public void setBatchInputFile(String batchInputFile) {
            this.batchInputFile = batchInputFile;
        }
    }
}
