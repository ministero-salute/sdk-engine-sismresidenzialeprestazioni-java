/* SPDX-License-Identifier: BSD-3-Clause */

package it.mds.sdk.flusso.sism.residenziale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@EnableWebMvc
@SpringBootApplication
@ComponentScan({"it.mds.sdk.flusso.sism.residenziale.controller", "it.mds.sdk.flusso.sism.residenziale","it.mds.sdk.rest.persistence.entity",
        "it.mds.sdk.libreriaregole.validator",
        "it.mds.sdk.flusso.sism.residenziale.service", "it.mds.sdk.flusso.sism.residenziale.tracciato",
        "it.mds.sdk.gestoreesiti", "it.mds.sdk.flusso.sism.residenziale.parser.regole","it.mds.sdk.flusso.sism.residenziale.parser.regole.conf",
        "it.mds.sdk.connettoremds"})


@OpenAPIDefinition(info=@Info(title = "SDK Ministero Della Salute - Flusso PSR", version = "0.0.5-SNAPSHOT", description = "Flusso Sism Residenziale"))
public class FlussoSismResidenziale {

    public static void main(String[] args) {
        SpringApplication.run(FlussoSismResidenziale.class, args);
    }

}
