package it.mds.sdk.flusso.sism.residenziale.tracciato;

import it.mds.sdk.flusso.sism.residenziale.parser.regole.RecordDtoSismResidenziale;
import it.mds.sdk.flusso.sism.residenziale.parser.regole.conf.ConfigurazioneFlussoSismRes;
import it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory;
import it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ResidenzialePrestazioniSanitarie;
import it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.TipoOperazione;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.tracciato.TracciatoSplitter;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("tracciatoSplitterSismRes")
public class TracciatoSplitterImpl implements TracciatoSplitter<RecordDtoSismResidenziale> {

    public ObjectFactory getObjFactory() {
        return new it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory();
    }

    public ConfigurazioneFlussoSismRes getConfigurazione() {
        return new ConfigurazioneFlussoSismRes();
    }
    @Override
    public List<Path> dividiTracciato(Path tracciato) {
        return null;
    }

    @Override
    public List<Path> dividiTracciato(List<RecordDtoSismResidenziale> records, String idRun) {

        try {

            ConfigurazioneFlussoSismRes conf = getConfigurazione();
            String annoRif = records.get(0).getAnnoRiferimento();
            String codiceRegione = records.get(0).getCodiceRegione();

            //XML PRESTAZIONI
            it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni = getObjFactory();
            ResidenzialePrestazioniSanitarie residenzialePrestazioniSanitarie = objPrestazioni.createResidenzialePrestazioniSanitarie();
            residenzialePrestazioniSanitarie.setAnnoRiferimento(annoRif);
            residenzialePrestazioniSanitarie.setCodiceRegione(codiceRegione);
            residenzialePrestazioniSanitarie.setPeriodoRiferimento(it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));

            for (RecordDtoSismResidenziale r : records) {
                if (!r.getTipoOperazionePrestazione().equalsIgnoreCase("NM")) {
                    creaPrestazioniXml(r, residenzialePrestazioniSanitarie, objPrestazioni);
                }
            }
            GestoreFile gestoreFile = GestoreFileFactory.getGestoreFile("XML");

            //recupero il path del file xsd di prestazioni
            URL resourcePrestazioni = this.getClass().getClassLoader().getResource("PSR.xsd");
            log.debug("URL dell'XSD per la validazione idrun {} : {}", idRun, resourcePrestazioni);

            //scrivi XML PRESTAZIONI
            String pathPrestazioni = conf.getXmlOutput().getPercorso() + "SDK_RES_PSR_" + records.get(0).getPeriodoRiferimento() + "_" + idRun + ".xml";
            gestoreFile.scriviDto(residenzialePrestazioniSanitarie, pathPrestazioni, resourcePrestazioni);

            return List.of(Path.of(pathPrestazioni));
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            log.error("[{}].dividiTracciato  - records[{}]  - idRun[{}] -" + e.getMessage(),
                    this.getClass().getName(),
                    records.stream().map(obj -> "" + obj.toString()).collect(Collectors.joining("|")),
                    idRun,
                    e
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossibile validare il csv in ingresso. message: " + e.getMessage());
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void creaPrestazioniXml(RecordDtoSismResidenziale r, ResidenzialePrestazioniSanitarie residenzialePrestazioniSanitarie,
                                    it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {

        //ASL RIF
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento currentAsl = getCurrentAsl(residenzialePrestazioniSanitarie, r);
        if (currentAsl == null) {
            currentAsl = creaAslPrestazioni(r.getCodiceAziendaSanitariaRiferimento(), objPrestazioni);
            residenzialePrestazioniSanitarie.getAziendaSanitariaRiferimento().add(currentAsl);
        }

        //DSM
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM currentDsm = getCurrentDsm(currentAsl, r);
        if (currentDsm == null) {
            currentDsm = creaDSMPrestazioni(r.getCodiceDipartimentoSaluteMentale(), objPrestazioni);
            currentAsl.getDSM().add(currentDsm);
        }

        //ASSISTITO
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito = getAssistito(currentDsm, r);
        if (currentAssisitito == null) {
            currentAssisitito = creaAssistitoPrestazioni(r, objPrestazioni);
            currentDsm.getAssistito().add(currentAssisitito);
        }

        //STRUTTURA
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura currentStruttura = getStruttura(currentAssisitito, r);
        if (currentStruttura == null) {
            currentStruttura = creaStrutturaPrestazioni(r, objPrestazioni);
            currentAssisitito.getStruttura().add(currentStruttura);
        }

        //CONTATTO
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto currentContatto = getContatto(currentStruttura, r);
        if (currentContatto == null) {
            currentContatto = creaContattoPrestazioni(r, objPrestazioni);
            currentStruttura.getContatto().add(currentContatto);
        }

        //PRESTAZIONI
        currentContatto.getPrestazioni().add(creaPrestazioni(r, objPrestazioni));

    }

    public ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto getContatto(
            ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura currentStruttura,
            RecordDtoSismResidenziale r
    ) {
        return currentStruttura.getContatto()
                .stream()
                .filter(cnt -> r.getIdContatto().equals(cnt.getIDContatto()))
                .findFirst()
                .orElse(null);
    }

    public ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura getStruttura(
            ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito,
            RecordDtoSismResidenziale r
    ) {
        return currentAssisitito.getStruttura()
                .stream()
                .filter(str -> r.getCodiceStruttura().equalsIgnoreCase(str.getCodiceStruttura()))
                .findFirst()
                .orElse(null);
    }

    public ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito getAssistito(
            ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM currentDsm,
            RecordDtoSismResidenziale r
    ) {
        return currentDsm.getAssistito()
                .stream()
                .filter(ass -> r.getIdRecord().equalsIgnoreCase(ass.getIdRec()))
                .findFirst()
                .orElse(null);
    }


    public ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM getCurrentDsm(
            ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento currentAsl,
            RecordDtoSismResidenziale r
    ) {
        return currentAsl.getDSM()
                .stream()
                .filter(dsm -> r.getCodiceDipartimentoSaluteMentale().equalsIgnoreCase(dsm.getCodiceDSM()))
                .findFirst()
                .orElse(null);
    }

    public ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento getCurrentAsl(
            ResidenzialePrestazioniSanitarie residenzialePrestazioniSanitarie,
            RecordDtoSismResidenziale r
    ) {
       return residenzialePrestazioniSanitarie.getAziendaSanitariaRiferimento()
                .stream()
                .filter(asl -> r.getCodiceAziendaSanitariaRiferimento().equalsIgnoreCase(asl.getCodiceAziendaSanitariaRiferimento()))
                .findFirst()
                .orElse(null);
    }


    private ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento creaAslPrestazioni(String codAsl,
                                                                                            it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento asl = objPrestazioni.createResidenzialePrestazioniSanitarieAziendaSanitariaRiferimento();
        asl.setCodiceAziendaSanitariaRiferimento(codAsl);
        return asl;
    }

    private ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM creaDSMPrestazioni(String codDsm,
                                                                                                it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM dsm = objPrestazioni.createResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSM();
        dsm.setCodiceDSM(codDsm);
        return dsm;
    }

    private ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito creaAssistitoPrestazioni(RecordDtoSismResidenziale r,
                                                                                                                it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito assistito = objPrestazioni.createResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistito();
        assistito.setIdRec(r.getIdRecord());
        return assistito;
    }

    private ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura creaStrutturaPrestazioni(RecordDtoSismResidenziale r,
                                                                                                                          it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura struttura = objPrestazioni.createResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistitoStruttura();
        struttura.setCodiceStruttura(r.getCodiceStruttura());
        return struttura;
    }

    private ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto creaContattoPrestazioni(RecordDtoSismResidenziale r,
                                                                                                                                  it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto contatto = objPrestazioni.createResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistitoStrutturaContatto();
        contatto.setIDContatto(r.getIdContatto());
        return contatto;
    }

    public ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto.Prestazioni creaPrestazioni(RecordDtoSismResidenziale r,
                                                                                                                                      it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.ObjectFactory objPrestazioni) {
        ResidenzialePrestazioniSanitarie.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto.Prestazioni prestazioni = objPrestazioni.createResidenzialePrestazioniSanitarieAziendaSanitariaRiferimentoDSMAssistitoStrutturaContattoPrestazioni();
        XMLGregorianCalendar dataAmm = null;
        XMLGregorianCalendar dataDimm = null;
        try {
            dataAmm = r.getDataAmmissione() != null ? DatatypeFactory.newInstance().newXMLGregorianCalendar(r.getDataAmmissione()) : null;
            dataDimm = r.getDataDimissione() != null ? DatatypeFactory.newInstance().newXMLGregorianCalendar(r.getDataDimissione()) : null;
        } catch (DatatypeConfigurationException e) {
            log.error("Errore conversione XMLGregorianCalendar date", e);
        }
        prestazioni.setDataAmmissione(dataAmm);
        prestazioni.setTipoOperazione(TipoOperazione.fromValue(r.getTipoOperazionePrestazione()));
        prestazioni.setDataDimissione(dataDimm);
        prestazioni.setTipoStrutturaResidenziale(r.getTipoStrutturaResidenziale());

        return prestazioni;
    }

    public ResidenzialePrestazioniSanitarie creaPrestazioniSanitarie(List<RecordDtoSismResidenziale> records, ResidenzialePrestazioniSanitarie prestazioniSanitarie) {

        //Imposto gli attribute element

        String annoRif = records.get(0).getAnnoRiferimento();
        String codiceRegione = records.get(0).getCodiceRegione();

        if (prestazioniSanitarie == null) {
            ObjectFactory objPrestSanitarie = getObjFactory();
            prestazioniSanitarie = objPrestSanitarie.createResidenzialePrestazioniSanitarie();
            prestazioniSanitarie.setAnnoRiferimento(annoRif);
            prestazioniSanitarie.setCodiceRegione(codiceRegione);
            prestazioniSanitarie.setPeriodoRiferimento(it.mds.sdk.flusso.sism.residenziale.tracciato.bean.output.prestazionisanitarie.PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));


            for (RecordDtoSismResidenziale r : records) {
                if (!r.getTipoOperazionePrestazione().equalsIgnoreCase("NM")) {
                    creaPrestazioniXml(r, prestazioniSanitarie, objPrestSanitarie);
                }
            }

        }
        return prestazioniSanitarie;
    }


}
