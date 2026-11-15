package com.casezero.casezero;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CaseCommentForm {

    @NotBlank(message = "Please write a comment before posting.")
    @Size(max = 1500, message = "Comments must be 1,500 characters or fewer.")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
