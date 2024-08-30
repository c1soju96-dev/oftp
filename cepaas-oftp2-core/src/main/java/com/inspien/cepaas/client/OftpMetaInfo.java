package com.inspien.cepaas.client;

import java.util.Date;

import org.neociclo.odetteftp.protocol.DeliveryNotification;
import org.neociclo.odetteftp.protocol.RecordFormat;
import org.neociclo.odetteftp.protocol.VirtualFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OftpMetaInfo {
    private String datasetName = "";
    private Date dateTime = new Date();
    private String destination = "";
    private String originator = "";
    private RecordFormat recordFormat = RecordFormat.UNSTRUCTURED;
    private Short ticker=0;

    public static OftpMetaInfo of(VirtualFile vf){
        OftpMetaInfo info = new OftpMetaInfo();
        info.setDatasetName(vf.getDatasetName());
        info.setDateTime(vf.getDateTime());
        info.setDestination(vf.getDestination());
        info.setOriginator(vf.getOriginator());
        info.setRecordFormat(RecordFormat.parse(vf.getRecordFormat().getCode()));
        info.setTicker(null == vf.getTicker() ? 0 : vf.getTicker());
        return info;
    }

    public static OftpMetaInfo of(DeliveryNotification vf){
        OftpMetaInfo info = new OftpMetaInfo();
        info.setDatasetName(vf.getDatasetName());
        info.setDateTime(vf.getDateTime());
        info.setDestination(vf.getDestination());
        info.setOriginator(vf.getOriginator());
        info.setTicker(null == vf.getTicker() ? 0 : vf.getTicker());
        return info;
    }
}

