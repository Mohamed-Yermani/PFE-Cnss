package com.projet.cnss.entity;

import lombok.*;

@Data

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

// Class
public class EmailDetails {


    private String recipient;
    private String msgBody;
    private String subject;
    private String attachment;
}
