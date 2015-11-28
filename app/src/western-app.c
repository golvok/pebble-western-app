/*
 * main.c
 * Constructs a Window housing an output TextLayer to show data from 
 * either modes of operation of the accelerometer.
 */

#include <pebble.h>
#include <math.h>
#include <inttypes.h>

#define TAP_NOT_DATA true

static Window *s_main_window;
static TextLayer *s_output_layer;
static void data_handler(AccelData *data, uint32_t num_samples);
static void main_window_load(Window *window);
static void main_window_unload(Window *window);
static void init();
static void deinit();
static uint32_t get_absolute_acceleration(int16_t x, int16_t y, int16_t z);

uint32_t points_examined = 0;
uint16_t n_spikes = 0;
#define SAMPLE_SIZE 10
#define NUMBER_OF_SAMPLE 15
// there will be a SAMPLE_SIZE * NUMBER_OF_SAMPLE points
#define SPIKE_THRESHOLD 20

static void data_handler(AccelData *data, uint32_t num_samples) {
	// Long lived buffer
	static char s_buffer[128];
	points_examined++;

	// snprintf(s_buffer, sizeof(s_buffer),
	// 	"%"PRIu32"\n%"PRIu32"\n%"PRIu32"\n%"PRIu32"\n%"PRIu32"\n",
	// 	get_absolute_acceleration(data[0].x, data[0].y, data[0].z),
	// 	get_absolute_acceleration(data[1].x, data[1].y, data[1].z),
	// 	get_absolute_acceleration(data[2].x, data[2].y, data[2].z),
	// 	get_absolute_acceleration(data[3].x, data[3].y, data[3].z),
	// 	get_absolute_acceleration(data[4].x, data[4].y, data[4].z)
	// );

	uint32_t i;
	for (i = 0; i < num_samples; i++) {
		uint32_t accel = get_absolute_acceleration(data[i].x, data[i].y, data[i].z);
		if (accel > 2000 || accel < 500) {
			n_spikes++;
		}		
	}

	uint16_t n_printed = snprintf(s_buffer, sizeof(s_buffer), 
		"points: \n%"PRIu32"\nspikes: %"PRIu16"\n", 
		points_examined,
		n_spikes
	); 

	if (points_examined == NUMBER_OF_SAMPLE) {
		if (n_spikes > SPIKE_THRESHOLD) {
			snprintf(s_buffer + n_printed, sizeof(s_buffer) - n_printed, "ALERT!!!");
		}
		points_examined = 0;
		n_spikes = 0;
	}

	//Show the data
	text_layer_set_text(s_output_layer, s_buffer);
}

static uint32_t get_absolute_acceleration(int16_t x, int16_t y, int16_t z){

	return (x*x + y*y + z*z)/1000;
}

static void main_window_load(Window *window) {
	Layer *window_layer = window_get_root_layer(window);
	GRect window_bounds = layer_get_bounds(window_layer);

	// Create output TextLayer
	s_output_layer = text_layer_create(GRect(5, 0, window_bounds.size.w - 10, window_bounds.size.h));
	text_layer_set_font(s_output_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24));
	text_layer_set_text(s_output_layer, "No data yet.");
	text_layer_set_overflow_mode(s_output_layer, GTextOverflowModeWordWrap);
	layer_add_child(window_layer, text_layer_get_layer(s_output_layer));
}

static void main_window_unload(Window *window) {
	// Destroy output TextLayer
	text_layer_destroy(s_output_layer);
}

static void init() {
	// Create main Window
	s_main_window = window_create();
	window_set_window_handlers(s_main_window, (WindowHandlers) {
		.load = main_window_load,
		.unload = main_window_unload
	});
	window_stack_push(s_main_window, true);

	// Subscribe to the accelerometer data service
	int num_samples = SAMPLE_SIZE;
	accel_data_service_subscribe(num_samples, data_handler);

	// Choose update rate
	accel_service_set_sampling_rate(ACCEL_SAMPLING_10HZ);
}

static void deinit() {
	// Destroy main Window
	window_destroy(s_main_window);

	if (TAP_NOT_DATA) {
		accel_tap_service_unsubscribe();
	} else {
		accel_data_service_unsubscribe();
	}
}

int main(void) {
	init();
	app_event_loop();
	deinit();
}