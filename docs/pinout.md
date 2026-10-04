# Hardware Pinout & Wiring Specifications

## Sipeed MaixCam (Sophgo SG2002) Pin Mapping

The peripheral pin assignments have been experimentally tested and validated for maximum signal integrity:

```
                  ┌─────────────────────────────────────────┐
                  │            SIPEED MAIXCAM               │
                  │                                         │
  UART1_RX (A18) ◄┼─── TF-Luna LiDAR TX (Green Wire)        │
  UART1_TX (A19) ─┼──► TF-Luna LiDAR RX (White Wire)        │
                  │                                         │
   I2C5_SCL(A15) ─┼──► ADS1115 ADC SCL                      │
   I2C5_SDA(A27) ◄┼──► ADS1115 ADC SDA                      │
                  │                                         │
        GPIO(A23) ◄┼─── Tactile Push Button (to GND)         │
                  │                                         │
      USB Type-C ─┼──► External USB DAC / Earphones         │
                  └─────────────────────────────────────────┘
```

### Critical Hardware Notes
- **Pins A16 & A17**: Reserved by the MaixPy system launcher comm protocol. Do not assign peripherals to these pins.
- **Pin A23**: Configured with internal pull-up (`gpio.Pull.PULL_UP`). Button press pulls logic level to `LOW` (`0`).
- **UART1**: Located at `/dev/ttyS1` at 115,200 baud, 8N1.
- **I2C5**: Accessible via native `maix.i2c.I2C(5)` or standard Linux `smbus(5)`.
