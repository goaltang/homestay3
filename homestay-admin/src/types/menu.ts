export interface Menus {
  id: string;
  title: string;
  index: string;
  icon?: string;
  permiss?: string;
  children?: MenusChildren[];
}

export interface MenusChildren {
  id: string;
  pid: string;
  index: string;
  title: string;
  permiss?: string;
  children?: MenusChildren[];
}
