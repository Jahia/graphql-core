import {registerRoutes as registerPlaygroundRoutes} from '././playground/registerRoutes';
import {registerRoutes as registerSDLRoutes} from '././sdl/registerRoutes';

export default function () {
    registerPlaygroundRoutes();
    registerSDLRoutes();
}
