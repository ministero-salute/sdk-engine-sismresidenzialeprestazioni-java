/* SPDX-License-Identifier: BSD-3-Clause */

package it.mds.sdk.flusso.sism.residenziale.parser.regole;

import com.opencsv.bean.CsvBindByPosition;
import it.mds.sdk.libreriaregole.dtos.RecordDtoGenerico;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordDtoSismResidenziale extends RecordDtoGenerico {

    //ANN_RIF~Periodo~COD_REG~COD_ASR_RFR~COD_DSM~ID_REC_KEYX~COD_STR~ID_CNT~TIP_STR_RES~DAT_AMM~DAT_DIM~TYP_OPR_PRES

    @CsvBindByPosition(position = 0)
    private String annoRiferimento; //ANN_RIF

    @CsvBindByPosition(position = 1)
    private String periodoRiferimento; //COD_PER

    @CsvBindByPosition(position = 2)
    private String codiceRegione; //COD_REG

    @CsvBindByPosition(position = 3)
    private String codiceAziendaSanitariaRiferimento; //COD_ASR_RFR

    @CsvBindByPosition(position = 4)
    private String codiceDipartimentoSaluteMentale;//COD_DSM

    @CsvBindByPosition(position = 5)
    private String idRecord;//ID_REC_KEYX

    @CsvBindByPosition(position = 6)
    private String codiceStruttura;//COD_STR

    @CsvBindByPosition(position = 7)
    private Long idContatto; //ID_CNT

    @CsvBindByPosition(position = 8)
    private String tipoStrutturaResidenziale; //TIP_STR_RES

    @CsvBindByPosition(position = 9)
    private String dataAmmissione; //DAT_AMM

    @CsvBindByPosition(position = 10)
    private String dataDimissione; //DAT_DIM

    @CsvBindByPosition(position = 11)
    private String tipoOperazionePrestazione; //TYP_OPR_PRES

}
