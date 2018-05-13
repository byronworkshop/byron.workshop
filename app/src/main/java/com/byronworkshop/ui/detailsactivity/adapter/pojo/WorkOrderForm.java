package com.byronworkshop.ui.detailsactivity.adapter.pojo;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

public class WorkOrderForm implements Serializable {

    // header
    private Date startDate;
    private Date endDate;
    private String issue;
    private int initialCompLevel;
    private int finalCompLevel;
    private String others;

    private int totalCost;
    private int imageCounter;

    // accessories
    private boolean rims;                   // aros
    private boolean ignition;               // arranque
    private boolean battery;                // batería
    private boolean horn;                   // bípode central
    private boolean centralBipod;           // bocina
    private boolean caps;                   // capuchones
    private boolean cdi;                    // CDI
    private boolean ignitionSwitch;         // chapa contacto
    private boolean clutchCable;            // chicotillo
    private boolean exhaust;                // escape
    private boolean headlight;              // farol
    private boolean filters;                // filtros
    private boolean lightBulbs;             // focos
    private boolean brakes;                 // frenos
    private boolean mudguard;               // guardabarros
    private boolean winkers;                // guiñadores
    private boolean tools;                  // herramientas
    private boolean emblems;                // insignias
    private boolean tires;                  // llantas
    private boolean key;                    // llave
    private boolean commands;               // mandos
    private boolean hoses;                  // mangueras
    private boolean handlebar;              // manillar
    private boolean seat;                   // montura
    private boolean catEyes;                // ojos de gato
    private boolean levers;                 // palancas
    private boolean racks;                  // parrillas
    private boolean paint;                  // pintura
    private boolean toolHolder;             // porta herramientas
    private boolean handGrips;              // puños
    private boolean radiator;               // radiador
    private boolean rectifier;              // rectificador
    private boolean rearviewMirrors;        // retrovisores
    private boolean electricSystem;         // sistema eléctrico
    private boolean licensePlateSupport;    // soporte de placa
    private boolean chainCover;             // tapa cadena
    private boolean fuelCap;                // tapa combustible
    private boolean valveCover;             // tapa válvulas
    private boolean engineCovers;           // tapas de motor
    private boolean sideCovers;             // tapas laterales
    private boolean upholstery;             // tapiz
    private boolean telescopicFork;         // telescopios
    private boolean chainTensioner;         // tesador cadena
    private boolean transmission;           // transmisión
    private boolean speedometer;            // velocímetro

    // preventive maintenance
    private boolean oil;                    // aceite
    private boolean engineTuning;           // afinado de motor
    private boolean boltAdjustment;         // ajuste de pernos
    private boolean changeSparkPlugs;       // cambio de bugias
    private boolean carburetion;            // carburación
    private boolean lubrication;            // engrase
    private boolean fusesRevision;          // revisión de fusibles
    private boolean transmissionCleaning;   // limpieza de transmisión
    private boolean tirePressure;           // presión de llantas
    private boolean breakAdjustment;        // reajuste de frenos

    // corrective maintenance
    private boolean pistonRings;            // anillas
    private boolean gearBox;                // caja de velocidades
    private boolean engineHead;             // culata
    private boolean clutchPlates;           // discos de embrague
    private boolean starter;                // motor de arranque
    private boolean piston;                 // pistón
    private boolean clutchPress;            // prensa de embrague
    private boolean grinding;               // rectificado
    private boolean solenoid;               // solenoide
    private boolean transmissionCorrection; // transmisión
    private boolean valves;                 // válvulas

    public WorkOrderForm() {
    }

    public WorkOrderForm(
            // header
            @NonNull Date startDate,
            Date endDate,
            @NonNull String issue,
            int initialCompLevel,
            int finalCompLevel,
            String others,
            int totalCost,
            int imageCounter,

            // accessories
            boolean rims,
            boolean ignition,
            boolean battery,
            boolean horn,
            boolean centralBipod,
            boolean caps,
            boolean cdi,
            boolean ignitionSwitch,
            boolean clutchCable,
            boolean exhaust,
            boolean headlight,
            boolean filters,
            boolean lightBulbs,
            boolean brakes,
            boolean mudguard,
            boolean winkers,
            boolean tools,
            boolean emblems,
            boolean tires,
            boolean key,
            boolean commands,
            boolean hoses,
            boolean handlebar,
            boolean seat,
            boolean catEyes,
            boolean levers,
            boolean racks,
            boolean paint,
            boolean toolHolder,
            boolean handGrips,
            boolean radiator,
            boolean rectifier,
            boolean rearviewMirrors,
            boolean electricSystem,
            boolean licensePlateSupport,
            boolean chainCover,
            boolean fuelCap,
            boolean valveCover,
            boolean engineCovers,
            boolean sideCovers,
            boolean upholstery,
            boolean telescopicFork,
            boolean chainTensioner,
            boolean transmission,
            boolean speedometer,

            // preventive maintenance
            boolean oil,
            boolean engineTuning,
            boolean boltAdjustment,
            boolean changeSparkPlugs,
            boolean carburetion,
            boolean lubrication,
            boolean fusesRevision,
            boolean transmissionCleaning,
            boolean tirePressure,
            boolean breakAdjustment,

            // corrective maintenance
            boolean pistonRings,
            boolean gearBox,
            boolean engineHead,
            boolean clutchPlates,
            boolean starter,
            boolean piston,
            boolean clutchPress,
            boolean grinding,
            boolean solenoid,
            boolean transmissionCorrection,
            boolean valves) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.issue = issue;
        this.initialCompLevel = initialCompLevel;
        this.finalCompLevel = finalCompLevel;
        this.others = others;
        this.totalCost = totalCost;
        this.imageCounter = imageCounter;

        // accessories
        this.rims = rims;
        this.ignition = ignition;
        this.battery = battery;
        this.horn = horn;
        this.centralBipod = centralBipod;
        this.caps = caps;
        this.cdi = cdi;
        this.ignitionSwitch = ignitionSwitch;
        this.clutchCable = clutchCable;
        this.exhaust = exhaust;
        this.headlight = headlight;
        this.filters = filters;
        this.lightBulbs = lightBulbs;
        this.brakes = brakes;
        this.mudguard = mudguard;
        this.winkers = winkers;
        this.tools = tools;
        this.emblems = emblems;
        this.tires = tires;
        this.key = key;
        this.commands = commands;
        this.hoses = hoses;
        this.handlebar = handlebar;
        this.seat = seat;
        this.catEyes = catEyes;
        this.levers = levers;
        this.racks = racks;
        this.paint = paint;
        this.toolHolder = toolHolder;
        this.handGrips = handGrips;
        this.radiator = radiator;
        this.rectifier = rectifier;
        this.rearviewMirrors = rearviewMirrors;
        this.electricSystem = electricSystem;
        this.licensePlateSupport = licensePlateSupport;
        this.chainCover = chainCover;
        this.fuelCap = fuelCap;
        this.valveCover = valveCover;
        this.engineCovers = engineCovers;
        this.sideCovers = sideCovers;
        this.upholstery = upholstery;
        this.telescopicFork = telescopicFork;
        this.chainTensioner = chainTensioner;
        this.transmission = transmission;
        this.speedometer = speedometer;

        // preventive maintenance
        this.oil = oil;
        this.engineTuning = engineTuning;
        this.boltAdjustment = boltAdjustment;
        this.changeSparkPlugs = changeSparkPlugs;
        this.carburetion = carburetion;
        this.lubrication = lubrication;
        this.fusesRevision = fusesRevision;
        this.transmissionCleaning = transmissionCleaning;
        this.tirePressure = tirePressure;
        this.breakAdjustment = breakAdjustment;

        // corrective maintenance
        this.pistonRings = pistonRings;
        this.gearBox = gearBox;
        this.engineHead = engineHead;
        this.clutchPlates = clutchPlates;
        this.starter = starter;
        this.piston = piston;
        this.clutchPress = clutchPress;
        this.grinding = grinding;
        this.solenoid = solenoid;
        this.transmissionCorrection = transmissionCorrection;
        this.valves = valves;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public boolean isClosed() {
        return endDate != null;
    }

    public String getIssue() {
        return issue;
    }

    public int getInitialCompLevel() {
        return initialCompLevel;
    }

    public int getFinalCompLevel() {
        return finalCompLevel;
    }

    public String getOthers() {
        return others;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public int getImageCounter() {
        return imageCounter;
    }

    public boolean isRims() {
        return rims;
    }

    public boolean isIgnition() {
        return ignition;
    }

    public boolean isBattery() {
        return battery;
    }

    public boolean isHorn() {
        return horn;
    }

    public boolean isCentralBipod() {
        return centralBipod;
    }

    public boolean isCaps() {
        return caps;
    }

    public boolean isCdi() {
        return cdi;
    }

    public boolean isIgnitionSwitch() {
        return ignitionSwitch;
    }

    public boolean isClutchCable() {
        return clutchCable;
    }

    public boolean isExhaust() {
        return exhaust;
    }

    public boolean isHeadlight() {
        return headlight;
    }

    public boolean isFilters() {
        return filters;
    }

    public boolean isLightBulbs() {
        return lightBulbs;
    }

    public boolean isBrakes() {
        return brakes;
    }

    public boolean isMudguard() {
        return mudguard;
    }

    public boolean isWinkers() {
        return winkers;
    }

    public boolean isTools() {
        return tools;
    }

    public boolean isEmblems() {
        return emblems;
    }

    public boolean isTires() {
        return tires;
    }

    public boolean isKey() {
        return key;
    }

    public boolean isCommands() {
        return commands;
    }

    public boolean isHoses() {
        return hoses;
    }

    public boolean isHandlebar() {
        return handlebar;
    }

    public boolean isSeat() {
        return seat;
    }

    public boolean isCatEyes() {
        return catEyes;
    }

    public boolean isLevers() {
        return levers;
    }

    public boolean isRacks() {
        return racks;
    }

    public boolean isPaint() {
        return paint;
    }

    public boolean isToolHolder() {
        return toolHolder;
    }

    public boolean isHandGrips() {
        return handGrips;
    }

    public boolean isRadiator() {
        return radiator;
    }

    public boolean isRectifier() {
        return rectifier;
    }

    public boolean isRearviewMirrors() {
        return rearviewMirrors;
    }

    public boolean isElectricSystem() {
        return electricSystem;
    }

    public boolean isLicensePlateSupport() {
        return licensePlateSupport;
    }

    public boolean isChainCover() {
        return chainCover;
    }

    public boolean isFuelCap() {
        return fuelCap;
    }

    public boolean isValveCover() {
        return valveCover;
    }

    public boolean isEngineCovers() {
        return engineCovers;
    }

    public boolean isSideCovers() {
        return sideCovers;
    }

    public boolean isUpholstery() {
        return upholstery;
    }

    public boolean isTelescopicFork() {
        return telescopicFork;
    }

    public boolean isChainTensioner() {
        return chainTensioner;
    }

    public boolean isTransmission() {
        return transmission;
    }

    public boolean isSpeedometer() {
        return speedometer;
    }

    public boolean isOil() {
        return oil;
    }

    public boolean isEngineTuning() {
        return engineTuning;
    }

    public boolean isBoltAdjustment() {
        return boltAdjustment;
    }

    public boolean isChangeSparkPlugs() {
        return changeSparkPlugs;
    }

    public boolean isCarburetion() {
        return carburetion;
    }

    public boolean isLubrication() {
        return lubrication;
    }

    public boolean isFusesRevision() {
        return fusesRevision;
    }

    public boolean isTransmissionCleaning() {
        return transmissionCleaning;
    }

    public boolean isTirePressure() {
        return tirePressure;
    }

    public boolean isBreakAdjustment() {
        return breakAdjustment;
    }

    public boolean isPistonRings() {
        return pistonRings;
    }

    public boolean isGearBox() {
        return gearBox;
    }

    public boolean isEngineHead() {
        return engineHead;
    }

    public boolean isClutchPlates() {
        return clutchPlates;
    }

    public boolean isStarter() {
        return starter;
    }

    public boolean isPiston() {
        return piston;
    }

    public boolean isClutchPress() {
        return clutchPress;
    }

    public boolean isGrinding() {
        return grinding;
    }

    public boolean isSolenoid() {
        return solenoid;
    }

    public boolean isTransmissionCorrection() {
        return transmissionCorrection;
    }

    public boolean isValves() {
        return valves;
    }
}
