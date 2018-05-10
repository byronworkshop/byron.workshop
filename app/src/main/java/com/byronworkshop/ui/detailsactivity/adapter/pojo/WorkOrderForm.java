package com.byronworkshop.ui.detailsactivity.adapter.pojo;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class WorkOrderForm implements Serializable {

    // header
    private long date;
    private String issue;
    private int initialCompLevel;
    private int finalCompLevel;
    private String others;

    private int totalCost;
    private int imageCounter;

    //accessories
    private boolean ignition;               // arranque
    private boolean battery;                // batería
    private boolean centralBipod;           // bípode central
    private boolean horn;                   // bocina
    private boolean cdi;                    // CDI
    private boolean headlight;              // farol
    private boolean lightBulbs;             // focos
    private boolean mudguard;               // guarda barros
    private boolean winkers;                // guiñadores
    private boolean paint;                  // pintura
    private boolean handGrips;              // puños
    private boolean rectifier;              // rectificador
    private boolean rearviewMirrors;        // retrovisores
    private boolean chainCover;             // tapa cadena
    private boolean telescopicFork;         // telescopios

    private boolean caps;                   // capuchones
    private boolean ignitionSwitch;         // chapa de contacto
    private boolean clutchCable;            // chicotillo
    private boolean exhaust;                // escape
    private boolean tires;                  // llantas
    private boolean key;                    // llave
    private boolean handlebar;              // manillar
    private boolean seat;                   // montura
    private boolean levers;                 // palancas
    private boolean racks;                  // parrillas
    private boolean radiator;               // radiador
    private boolean licensePlateSupport;    // soporte de placa
    private boolean valveCover;             // tapa válvulas
    private boolean upholstery;             // tapiz
    private boolean chainTensioner;         // tesador de cadena

    private boolean rims;                   // aros
    private boolean filters;                // filtros
    private boolean brakes;                 // frenos
    private boolean tools;                  // herramientas
    private boolean emblems;                // insignias
    private boolean commands;               // mandos
    private boolean hoses;                  // mangueras
    private boolean catEyes;                // ojos de gato ****
    private boolean toolHolder;             // porta herramientas
    private boolean electricSystem;         // sistema eléctrico
    private boolean fuelCap;                // tapa de combustible
    private boolean engineCovers;           // tapas de motor
    private boolean sideCovers;             // tapas laterales
    private boolean transmission;           // transmisión
    private boolean speedometer;            // velocímetro

    // preventive maintenance
    private boolean oil;                    // aceite
    private boolean lubrication;            // engrase
    private boolean boltAdjustment;         // ajuste de pernos
    private boolean engineTuning;           // afinado de motor
    private boolean carburetion;            // carburación
    private boolean changeSparkPlugs;       // cambio de bugias
    private boolean transmissionCleaning;   // limpieza de transmisión
    private boolean breakAdjustment;         // reajuste de frenos
    private boolean tirePressure;           // presión de llantas
    private boolean fusesRevision;          // revisión de fusibles

    // corrective maintenance
    private boolean valves;                 // válvulas
    private boolean pistonRings;            // anillas
    private boolean piston;                 // pistón
    private boolean transmissionCorrection; // transmisión
    private boolean clutchPlates;           // discos de embrague
    private boolean engineHead;             // culata
    private boolean clutchPress;            // prensa de embrague
    private boolean starter;                // motor de arranque
    private boolean solenoid;               // solenoide
    private boolean gearBox;                // caja de velocidades
    private boolean grinding;               // rectificado

    public WorkOrderForm() {
    }

    public WorkOrderForm(
            // header
            long date,
            @NonNull String issue,
            int initialCompLevel,
            int finalCompLevel,
            String others,
            int totalCost,
            int imageCounter,

            // accessories
            boolean ignition,
            boolean battery,
            boolean centralBipod,
            boolean horn,
            boolean cdi,
            boolean headlight,
            boolean lightBulbs,
            boolean mudguard,
            boolean winkers,
            boolean paint,
            boolean handGrips,
            boolean rectifier,
            boolean rearviewMirrors,
            boolean chainCover,
            boolean telescopicFork,

            boolean caps,
            boolean ignitionSwitch,
            boolean clutchCable,
            boolean exhaust,
            boolean tires,
            boolean key,
            boolean handlebar,
            boolean seat,
            boolean levers,
            boolean racks,
            boolean radiator,
            boolean licensePlateSupport,
            boolean valveCover,
            boolean upholstery,
            boolean chainTensioner,

            boolean rims,
            boolean filters,
            boolean brakes,
            boolean tools,
            boolean emblems,
            boolean commands,
            boolean hoses,
            boolean catEyes,
            boolean toolHolder,
            boolean electricSystem,
            boolean fuelCap,
            boolean engineCovers,
            boolean sideCovers,
            boolean transmission,
            boolean speedometer,

            // preventive maintenance
            boolean oil,
            boolean lubrication,
            boolean boltAdjustment,
            boolean engineTuning,
            boolean carburetion,
            boolean changeSparkPlugs,
            boolean transmissionCleaning,
            boolean breakAdjustment,
            boolean tirePressure,
            boolean fusesRevision,

            // corrective maintenance
            boolean valves,
            boolean pistonRings,
            boolean piston,
            boolean transmissionCorrection,
            boolean clutchPlates,
            boolean engineHead,
            boolean clutchPress,
            boolean starter,
            boolean solenoid,
            boolean gearBox,
            boolean grinding) {
        this.date = date;
        this.issue = issue;
        this.initialCompLevel = initialCompLevel;
        this.finalCompLevel = finalCompLevel;
        this.others = others;
        this.totalCost = totalCost;
        this.imageCounter = imageCounter;

        this.ignition = ignition;
        this.battery = battery;
        this.centralBipod = centralBipod;
        this.horn = horn;
        this.cdi = cdi;
        this.headlight = headlight;
        this.lightBulbs = lightBulbs;
        this.mudguard = mudguard;
        this.winkers = winkers;
        this.paint = paint;
        this.handGrips = handGrips;
        this.rectifier = rectifier;
        this.rearviewMirrors = rearviewMirrors;
        this.chainCover = chainCover;
        this.telescopicFork = telescopicFork;
        this.caps = caps;
        this.ignitionSwitch = ignitionSwitch;
        this.clutchCable = clutchCable;
        this.exhaust = exhaust;
        this.tires = tires;
        this.key = key;
        this.handlebar = handlebar;
        this.seat = seat;
        this.levers = levers;
        this.racks = racks;
        this.radiator = radiator;
        this.licensePlateSupport = licensePlateSupport;
        this.valveCover = valveCover;
        this.upholstery = upholstery;
        this.chainTensioner = chainTensioner;
        this.rims = rims;
        this.filters = filters;
        this.brakes = brakes;
        this.tools = tools;
        this.emblems = emblems;
        this.commands = commands;
        this.hoses = hoses;
        this.catEyes = catEyes;
        this.toolHolder = toolHolder;
        this.electricSystem = electricSystem;
        this.fuelCap = fuelCap;
        this.engineCovers = engineCovers;
        this.sideCovers = sideCovers;
        this.transmission = transmission;
        this.speedometer = speedometer;
        this.oil = oil;
        this.lubrication = lubrication;
        this.boltAdjustment = boltAdjustment;
        this.engineTuning = engineTuning;
        this.carburetion = carburetion;
        this.changeSparkPlugs = changeSparkPlugs;
        this.transmissionCleaning = transmissionCleaning;
        this.breakAdjustment = breakAdjustment;
        this.tirePressure = tirePressure;
        this.fusesRevision = fusesRevision;
        this.valves = valves;
        this.pistonRings = pistonRings;
        this.piston = piston;
        this.transmissionCorrection = transmissionCorrection;
        this.clutchPlates = clutchPlates;
        this.engineHead = engineHead;
        this.clutchPress = clutchPress;
        this.starter = starter;
        this.solenoid = solenoid;
        this.gearBox = gearBox;
        this.grinding = grinding;
    }

    public long getDate() {
        return date;
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

    public boolean isIgnition() {
        return ignition;
    }

    public boolean isBattery() {
        return battery;
    }

    public boolean isCentralBipod() {
        return centralBipod;
    }

    public boolean isHorn() {
        return horn;
    }

    public boolean isCdi() {
        return cdi;
    }

    public boolean isHeadlight() {
        return headlight;
    }

    public boolean isLightBulbs() {
        return lightBulbs;
    }

    public boolean isMudguard() {
        return mudguard;
    }

    public boolean isWinkers() {
        return winkers;
    }

    public boolean isPaint() {
        return paint;
    }

    public boolean isHandGrips() {
        return handGrips;
    }

    public boolean isRectifier() {
        return rectifier;
    }

    public boolean isRearviewMirrors() {
        return rearviewMirrors;
    }

    public boolean isChainCover() {
        return chainCover;
    }

    public boolean isTelescopicFork() {
        return telescopicFork;
    }

    public boolean isCaps() {
        return caps;
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

    public boolean isTires() {
        return tires;
    }

    public boolean isKey() {
        return key;
    }

    public boolean isHandlebar() {
        return handlebar;
    }

    public boolean isSeat() {
        return seat;
    }

    public boolean isLevers() {
        return levers;
    }

    public boolean isRacks() {
        return racks;
    }

    public boolean isRadiator() {
        return radiator;
    }

    public boolean isLicensePlateSupport() {
        return licensePlateSupport;
    }

    public boolean isValveCover() {
        return valveCover;
    }

    public boolean isUpholstery() {
        return upholstery;
    }

    public boolean isChainTensioner() {
        return chainTensioner;
    }

    public boolean isRims() {
        return rims;
    }

    public boolean isFilters() {
        return filters;
    }

    public boolean isBrakes() {
        return brakes;
    }

    public boolean isTools() {
        return tools;
    }

    public boolean isEmblems() {
        return emblems;
    }

    public boolean isCommands() {
        return commands;
    }

    public boolean isHoses() {
        return hoses;
    }

    public boolean isCatEyes() {
        return catEyes;
    }

    public boolean isToolHolder() {
        return toolHolder;
    }

    public boolean isElectricSystem() {
        return electricSystem;
    }

    public boolean isFuelCap() {
        return fuelCap;
    }

    public boolean isEngineCovers() {
        return engineCovers;
    }

    public boolean isSideCovers() {
        return sideCovers;
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

    public boolean isLubrication() {
        return lubrication;
    }

    public boolean isBoltAdjustment() {
        return boltAdjustment;
    }

    public boolean isEngineTuning() {
        return engineTuning;
    }

    public boolean isCarburetion() {
        return carburetion;
    }

    public boolean isChangeSparkPlugs() {
        return changeSparkPlugs;
    }

    public boolean isTransmissionCleaning() {
        return transmissionCleaning;
    }

    public boolean isBreakAdjustment() {
        return breakAdjustment;
    }

    public boolean isTirePressure() {
        return tirePressure;
    }

    public boolean isFusesRevision() {
        return fusesRevision;
    }

    public boolean isValves() {
        return valves;
    }

    public boolean isPistonRings() {
        return pistonRings;
    }

    public boolean isPiston() {
        return piston;
    }

    public boolean isTransmissionCorrection() {
        return transmissionCorrection;
    }

    public boolean isClutchPlates() {
        return clutchPlates;
    }

    public boolean isEngineHead() {
        return engineHead;
    }

    public boolean isClutchPress() {
        return clutchPress;
    }

    public boolean isStarter() {
        return starter;
    }

    public boolean isSolenoid() {
        return solenoid;
    }

    public boolean isGearBox() {
        return gearBox;
    }

    public boolean isGrinding() {
        return grinding;
    }
}
