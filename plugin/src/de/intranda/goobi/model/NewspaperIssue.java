package de.intranda.goobi.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
public class NewspaperIssue {

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    public static final DateFormat dateYearFormat = new SimpleDateFormat("yyyy");
    public static final DateFormat dateMonthFormat = new SimpleDateFormat("MM");
    public static final DateFormat dateDayFormat = new SimpleDateFormat("dd");
    //	public static final NumberFormat issueNumberFormat = new DecimalFormat("0");


    private Newspaper newspaper;
    private String issueNumber;
    private Date issueDate;
    private String issueComment;
    private IssueType issueType;
    private String imageUploadFolderName;
    private List<FileUpload> files = new ArrayList<>();

    public NewspaperIssue(Newspaper newspaper) {
        if(newspaper == null) {
            throw new NullPointerException("Newspaper may not be null");
        }
        this.newspaper = newspaper;
    }

    public Map<String, String> getMetadataMap() {
        HashMap<String, String> map = new HashMap<>();
        if(issueNumber != null) {
            map.put("issueNumber", issueNumber);
        }
        if(issueDate != null) {
            map.put("issueDate", dateFormat.format(issueDate));
        }
        if(issueComment != null) {
            map.put("issueComment", issueComment);
        }
        return map;
    }

    public void setIssueComment(String comment) {
        if(comment.matches("\\d")) {
            IssueType type = IssueType.get(Integer.parseInt(comment));
            if(type != null) {
                comment = type.name;
            } else {

            }
        }
        this.issueComment  = comment;
    }

    public static enum IssueType {
        printedIssue(1, "Printed issue"),
        digitalIssue(2, "Digital issue"),
        printedIssueConv(3, "Converting printed issue"),
        digitalIssueConv(4, "Converting digital issue");

        public int code;
        public String name;

        private IssueType(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getCode() {
            return code;
        }

        public static IssueType get(int code) {
            for(IssueType type : IssueType.values()) {
                if(type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown issue type: " + code);
        }

        public static IssueType get(String name) {
            for(IssueType type : IssueType.values()) {
                if(type.name.equals(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown issue type: " + name);
        }
    }

    public boolean hasIssueNumber() {
        return StringUtils.isNotBlank(issueNumber);
    }

    public void setImageUploadFolderName(String folderName) {
        this.imageUploadFolderName = folderName;
    }

    public String getImageUploadFolderName() {
        return this.imageUploadFolderName;
    }

}
