package org.pentaho.pms.ui;

/**
 * ©Copyright 2024 BMC Software. Inc.
 */

import java.util.List;

/**
 * @author Yogesh
 */
public class ListFilesResponse {
    private List<String> ootb;
    private List<String> others;
    private List<String> deleted;

    public List<String> getOotb() {
        return ootb;
    }

    public void setOotb(List<String> ootb) {
        this.ootb = ootb;
    }

    public List<String> getOthers() {
        return others;
    }

    public void setOthers(List<String> others) {
        this.others = others;
    }

    public List<String> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<String> deleted) {
        this.deleted = deleted;
    }
}
