/*
 main.c
 LED demo -- stripped down version
 Greg Cook July 2015 v4
*/

#include <stdint.h>
#include "water.h"

#define GPIOA_BASE          	(AHB1PERIPH_BASE + 0x0000)
#define GPIOD_BASE		(AHB1PERIPH_BASE + 0x0C00)

#define RCC_AHB1Periph_GPIOA	((uint32_t)0x00000001)
#define RCC_AHB1Periph_GPIOD	((uint32_t)0x00000008)
#define GPIO_Mode_OUT		((uint32_t)0x01)

#define GPIO_Pin_12		((uint16_t)1<<12)  
#define GPIO_Pin_13		((uint16_t)1<<13)  
#define GPIO_Pin_14		((uint16_t)1<<14)  
#define GPIO_Pin_15		((uint16_t)1<<15)  

struct GPIO_t
{
  volatile uint32_t MODER;    /*!< GPIO port mode register,               offset: 0x00      */
  volatile uint32_t OTYPER;   /*!< GPIO port output type register,        offset: 0x04      */
  volatile uint32_t OSPEEDR;  /*!< GPIO port output speed register,       offset: 0x08      */
  volatile uint32_t PUPDR;    /*!< GPIO port pull-up/pull-down register,  offset: 0x0C      */
  volatile uint32_t IDR;      /*!< GPIO port input data register,         offset: 0x10      */
  volatile uint32_t ODR;      /*!< GPIO port output data register,        offset: 0x14      */
  volatile uint16_t BSRRL;    /*!< GPIO port bit set/reset low register,  offset: 0x18      */
  volatile uint16_t BSRRH;    /*!< GPIO port bit set/reset high register, offset: 0x1A      */
  volatile uint32_t LCKR;     /*!< GPIO port configuration lock register, offset: 0x1C      */
  volatile uint32_t AFR[2];   /*!< GPIO alternate function registers,     offset: 0x20-0x24 */
};

void Delay(__IO uint32_t nCount);
void init_leds(void);
void blink_all(int delay);
void blink_ring_v0(int delay);

extern struct GPIO_t *GPIOA;
extern struct GPIO_t *GPIOD;

struct GPIO_t *GPIOA = (struct GPIO_t*)GPIOA_BASE;
struct GPIO_t *GPIOD = (struct GPIO_t*)GPIOD_BASE;

int main(void)
{
  RCC->AHB1ENR |= RCC_AHB1Periph_GPIOA;

  init_leds();
  SysTick_Config(SystemCoreClock / 1000);

  int i;
  for (i = 0; i < 5; ++i)
    blink_all(50);

  while(1)
    blink_ring_v0(250);
}

void blink_all(int delay)
{
  GPIOD->BSRRL = GPIO_Pin_12 | GPIO_Pin_13 | GPIO_Pin_14 | GPIO_Pin_15;
  Delay(delay);
  GPIOD->BSRRH = GPIO_Pin_12 | GPIO_Pin_13 | GPIO_Pin_14 | GPIO_Pin_15;  
  Delay(delay);
}

void blink_ring_v0(int delay)
{
  GPIOD->BSRRL = GPIO_Pin_12;
  Delay(delay);

  /* reset pins */
  GPIOD->BSRRH = GPIO_Pin_12;
  Delay(delay);
}

void init_leds(void)
{
  RCC->AHB1ENR |= RCC_AHB1Periph_GPIOD;

  /* Configure PD12, PD13, PD14 and PD15 in output pushpull mode */
  uint32_t pins = GPIO_Pin_12|GPIO_Pin_13|GPIO_Pin_14|GPIO_Pin_15;
  uint32_t pin;
  for (pin = 0; pin < 16; ++pin)
  {
    if (!((((uint32_t)0x01) << pin) & pins)) continue;
    // set mode to output -- have to configure to out 
    GPIOD->MODER |= (((uint32_t)GPIO_Mode_OUT) << (pin * 2));
  }
}

/* uses __IO (volatile) to prevent empty while loop from being optimized away */
/* ms delay timer */
static volatile uint32_t TimingDelay;
void Delay(volatile uint32_t nTime)
{ 
  TimingDelay = nTime;

  while(TimingDelay != 0);
}

void SysTick_Handler(void)
{
  if (TimingDelay != 0x00)
  { 
    TimingDelay--;
  }
}
